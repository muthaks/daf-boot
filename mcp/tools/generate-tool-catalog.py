#!/usr/bin/env python3
"""Generate the MCP tool catalog (mcp/src/main/resources/daf-tools.json) from the daf-api resources.

Every JAX-RS endpoint daf-api serves becomes one tool: its name, the Swagger description the resource
carries, verb, path, parameters and the request body's fields. The MCP server reads the catalog and
turns each entry into a tool that calls daf-api with the caller's bearer token. Nothing here calls an
API or a database.

    python mcp/tools/generate-tool-catalog.py           # rewrite the catalog
    python mcp/tools/generate-tool-catalog.py --check   # exit 1 when the catalog is out of date

Request body fields come from the DTO sources in api/ where they exist, otherwise from the legacy
model jar (legacy-libs/target, built by the Maven build) through javap.
"""
import json
import os
import re
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RESOURCES = "api/src/main/java/za/co/rmb/approvalforum/web/resources"
PLATFORM = "api/src/main/java/za/co/rmb/credit/daf/platform"
SOURCES = ["api/src/main/java"]
LEGACY_MODEL_JAR = "legacy-libs/target/daf-legacy-libs-1.0.0-SNAPSHOT-approvalforum-model.jar"
OUT = "mcp/src/main/resources/daf-tools.json"

VERBS = ("GET", "POST", "PUT", "DELETE", "PATCH")

# Resource class -> tool name prefix. The prefix is the area of DAF the tool belongs to.
GROUPS = {
    "ApprovalForumAgendaResource": "agenda",
    "ApprovalForumResource": "workflow",
    "ApprovalForumAuthResource": "auth",
    "ApprovalForumExternalResource": "external",
    "ApprovalForumCreditDataResource": "creditdata",
    "ApprovalForumDocumentsResource": "documents",
    "ApprovalForumDocstashResource": "docstash",
    "ApprovalForumEmailReceivedResource": "email",
    "ApprovalForumSchedulerResource": "scheduler",
    "GitResource": "git",
    "PlatformTokenResource": "auth",
}

# Endpoints that are not for a user's agent: the scheduled jobs and their triggers, the health
# probe that starts the scheduler, and the build information. They stay in the catalog in the
# "system" category, which the server leaves out unless configured to serve it.
SYSTEM = {
    "POST /approvalForum/agenda/processCutOff",
    "POST /approvalForum/agenda/processReminders",
    "POST /approvalForum/request/external/process",
    "POST /approvalForum/request/cleanup",
    "POST /approvalForum/mail/escalations",
    "POST /approvalForum/mail/approver",
    "POST /approvalForum/mail/capturer",
    "GET /approvalForum/mail/test",
    "POST /approvalForum/mail/sendUnsent",
    "POST /approvalForum/mail/resendDaily",
    "POST /docstash/upload",
    "POST /docstash/process",
    "POST /docstash/adu/cleanup",
    "GET /emailReceived/ping",
    "GET /emailReceived/process",
    "GET /scheduler/scheduler",
    "GET /git",
    "GET /auth/error",
}

# Sign-in endpoints: they take credentials or another system's token instead of a DAF bearer.
# Category "sign-in"; the server leaves them out unless configured to serve them.
SIGN_IN = {
    "POST /auth/login",
    "POST /auth/roleToken",
    "POST /auth/ssoToken",
    "POST /auth/tenant",
    "GET /auth/keycloakConfig",
    "POST /auth/platformToken",
}

# Endpoints without a Swagger description in the resource: described here instead.
DESCRIPTIONS = {
    "POST /approvalForum/agenda/items/{agiId}/reviewer": "Reassign the review analyst of an agenda item",
    "POST /approvalForum/agenda/meetings/{agmId}/invitation": "Send the calendar invitations for a meeting's approved items",
    "POST /approvalForum/agenda/meetings/{agmId}/invitation/update": "Send updated calendar invitations after a meeting changed",
    "POST /auth/platformToken": "DAF session for a user signed in to the Credit platform: the bearer is the platform token exchanged for DAF; answers DAF's own token and tenant",
    "GET /approvalForum/mail/test": "Send a test mail",
}

JSON_TYPES = {
    "int": "integer", "long": "integer", "short": "integer", "byte": "integer",
    "Integer": "integer", "Long": "integer", "Short": "integer", "Byte": "integer", "BigInteger": "integer",
    "double": "number", "float": "number", "Double": "number", "Float": "number", "BigDecimal": "number",
    "boolean": "boolean", "Boolean": "boolean",
    "String": "string", "char": "string", "Character": "string", "UUID": "string",
    "Object": "object",
}
DATE_TYPES = {"Timestamp", "Date", "LocalDate", "LocalDateTime", "LocalTime", "ZonedDateTime",
              "OffsetDateTime", "Instant", "XMLGregorianCalendar"}
MAX_DEPTH = 4


def read(rel):
    with open(os.path.join(ROOT, rel), encoding="utf-8") as handle:
        return handle.read().replace("\r\n", "\n")


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def normalise(path):
    path = re.sub(r"\{\s*(\w+)\s*(:[^}]*)?\}", r"{\1}", path)
    return "/" + "/".join(p for p in path.split("/") if p)


def snake(name):
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def string_literal(text):
    """Join a Java string expression made of concatenated literals into one string."""
    parts = re.findall(r'"((?:[^"\\]|\\.)*)"', text)
    return " ".join(p.replace('\\"', '"') for p in parts).replace("  ", " ").strip()


def balanced(src, start):
    """Return the text of the parenthesised group starting at src[start] == '('."""
    depth = 0
    for i in range(start, len(src)):
        if src[i] == "(":
            depth += 1
        elif src[i] == ")":
            depth -= 1
            if depth == 0:
                return src[start + 1:i]
    raise ValueError("unbalanced parentheses")


# ---- DTO fields -------------------------------------------------------------------------------

_source_index = None


def source_index():
    global _source_index
    if _source_index is None:
        _source_index = {}
        for base in SOURCES:
            for folder, _, files in os.walk(os.path.join(ROOT, base)):
                for name in files:
                    if name.endswith(".java"):
                        _source_index.setdefault(name[:-5], os.path.join(folder, name))
    return _source_index


_javap_cache = {}


def legacy_fields(simple_name):
    """Fields of a legacy model class, through javap on the model jar: [(type, name)] or None."""
    if simple_name in _javap_cache:
        return _javap_cache[simple_name]
    jar = os.path.join(ROOT, LEGACY_MODEL_JAR)
    result = None
    if os.path.exists(jar):
        fqcn = "za.co.rmb.approvalforum.model." + simple_name
        try:
            out = subprocess.run(["javap", "-p", "-cp", jar, fqcn], capture_output=True, text=True, timeout=60)
            if out.returncode == 0:
                if re.search(r"\bfinal class \S+ extends java\.lang\.Enum", out.stdout):
                    result = "enum"
                else:
                    result = []
                    for m in re.finditer(r"^\s+(?:private|protected|public)?\s*(?!static)([\w.<>, ?\[\]]+?)\s+(\w+);", out.stdout, re.M):
                        type_text, name = m.group(1).strip(), m.group(2)
                        if "serialVersionUID" in name:
                            continue
                        result.append((type_text.replace("java.lang.", "").replace("java.util.", "")
                                       .replace("java.time.", "").replace("java.sql.", "")
                                       .replace("za.co.rmb.approvalforum.model.", ""), name))
        except (OSError, subprocess.SubprocessError):
            result = None
    _javap_cache[simple_name] = result
    return result


def source_fields(simple_name):
    """Fields of a DTO in the api sources: [(type, name)], 'enum', or None when the class is not there."""
    path = source_index().get(simple_name)
    if not path:
        return None
    src = strip_comments(open(path, encoding="utf-8").read())
    if re.search(r"\benum\s+" + simple_name + r"\b", src):
        return "enum"
    body_start = src.find("{", src.find("class " + simple_name))
    body = src[body_start + 1:]
    fields = []
    for m in re.finditer(r"^\s+(?:private|protected|public)\s+(?!static)(?!final\s+static)([\w<>, ?\[\]]+?)\s+(\w+)\s*(?:=[^;]*)?;", body, re.M):
        type_text, name = m.group(1).strip(), m.group(2)
        if type_text.startswith("final "):
            type_text = type_text[6:]
        fields.append((type_text, name))
    return fields


def schema_for_type(type_text, depth):
    type_text = type_text.strip()
    generic = re.match(r"^(\w+)\s*<\s*(.+)\s*>$", type_text)
    if type_text.endswith("[]"):
        return {"type": "array", "items": schema_for_type(type_text[:-2], depth)}
    if generic:
        outer, inner = generic.group(1), generic.group(2)
        if outer in ("List", "Set", "Collection", "ArrayList", "Iterable"):
            return {"type": "array", "items": schema_for_type(inner, depth)}
        if outer in ("Map", "HashMap"):
            return {"type": "object", "additionalProperties": True}
        return schema_for_type(inner, depth)
    if type_text in JSON_TYPES:
        return {"type": JSON_TYPES[type_text]}
    if type_text in DATE_TYPES:
        return {"type": "string", "description": "Date or time, ISO 8601"}
    if type_text in ("List", "Set", "Collection"):
        return {"type": "array", "items": {}}
    if type_text in ("Map",):
        return {"type": "object", "additionalProperties": True}
    return schema_for_class(type_text, depth)


def schema_for_class(simple_name, depth):
    fields = source_fields(simple_name)
    if fields is None:
        fields = legacy_fields(simple_name)
    if fields == "enum":
        return {"type": "string", "description": simple_name}
    if fields is None:
        return {"type": "object", "description": simple_name, "additionalProperties": True}
    if depth >= MAX_DEPTH:
        return {"type": "object", "description": simple_name, "additionalProperties": True}
    properties = {}
    for type_text, name in fields:
        properties[name] = schema_for_type(type_text, depth + 1)
    return {"type": "object", "description": simple_name, "properties": properties, "additionalProperties": True}


# ---- Resource scanning --------------------------------------------------------------------------

def parse_parameters(params_text):
    """Split a method's parameter list into (annotations, type, name) triples."""
    params = []
    depth = 0
    current = ""
    for ch in params_text:
        if ch in "(<":
            depth += 1
        elif ch in ")>":
            depth -= 1
        if ch == "," and depth == 0:
            params.append(current.strip())
            current = ""
        else:
            current += ch
    if current.strip():
        params.append(current.strip())
    out = []
    for p in params:
        p = " ".join(p.split())
        m = re.match(r"^((?:@\w+(?:\((?:[^()]|\([^()]*\))*\))?\s*)*)(?:final\s+)?([\w.<>, ?\[\]]+?)\s+(\w+)$", p)
        if not m:
            raise SystemExit(f"Cannot parse parameter: {p}")
        out.append((m.group(1), m.group(2), m.group(3)))
    return out


def query_type(java_type):
    return {"int": "integer", "long": "integer", "Integer": "integer", "Long": "integer",
            "boolean": "boolean", "Boolean": "boolean", "double": "number", "float": "number"}.get(java_type, "string")


def scan(rel, class_name):
    src = strip_comments(read(rel))
    cls = re.search(r"@Path\(\s*([^)]*)\)[^{;]*?class\s+" + class_name, src, re.S)
    if not cls:
        return []
    class_path_expr = cls.group(1)
    if re.search(r"\bPATH\b", class_path_expr):
        # @Path("/" + PlatformTokenResource.PATH): the constant is declared in the class
        const = re.search(r'static final String PATH = "([^"]+)"', src)
        class_path = "/" + const.group(1)
    else:
        class_path = string_literal(class_path_expr)
    tools = []
    for m in re.finditer(r"((?:@\w+(?:\((?:[^()]|\([^()]*\))*\))?\s*)+)public\s+[\w<>?, ]+\s+(\w+)\s*\(", src):
        annotations, method_name = m.group(1), m.group(2)
        verb = next((v for v in VERBS if re.search(r"@" + v + r"\b", annotations)), None)
        if not verb:
            continue
        p = re.search(r"@Path\(\s*\"([^\"]*)\"", annotations)
        path = normalise(class_path + "/" + (p.group(1) if p else ""))
        op = re.search(r"@ApiOperation\(", annotations)
        description = ""
        if op:
            args = balanced(annotations, op.end() - 1)
            value = re.search(r"value\s*=\s*((?:\"(?:[^\"\\]|\\.)*\"\s*\+?\s*)+)", args)
            description = string_literal(value.group(1)) if value else ""
            notes = re.search(r"notes\s*=\s*((?:\"(?:[^\"\\]|\\.)*\"\s*\+?\s*)+)", args)
            if notes and string_literal(notes.group(1)):
                description += " " + string_literal(notes.group(1))
        consumes = re.search(r"@Consumes\(\s*\"([^\"]*)\"", annotations)
        produces = re.search(r"@Produces\(\s*([^)]*)\)", annotations)
        multipart = bool(consumes and "multipart" in consumes.group(1))
        binary = bool(produces and "OCTET_STREAM" in produces.group(1))
        permit_all = "@PermitAll" in annotations
        params_text = balanced(src, m.end() - 1)
        path_params, query_params, header_params, body = [], [], [], None
        for ann, java_type, name in parse_parameters(params_text):
            if "@Context" in ann:
                continue
            default = re.search(r'@DefaultValue\(\s*"([^"]*)"', ann)
            required_api = bool(re.search(r"required\s*=\s*true", ann))
            if "@PathParam" in ann:
                path_params.append({"name": re.search(r'@PathParam\(\s*"([^"]+)"', ann).group(1),
                                    "type": query_type(java_type)})
            elif "@QueryParam" in ann:
                query_params.append({"name": re.search(r'@QueryParam\(\s*"([^"]+)"', ann).group(1),
                                     "type": query_type(java_type),
                                     "default": default.group(1) if default else None,
                                     "required": required_api})
            elif "@HeaderParam" in ann:
                header_params.append({"name": re.search(r'@HeaderParam\(\s*"([^"]+)"', ann).group(1),
                                      "required": required_api})
            elif java_type == "MultipartFormDataInput":
                multipart = True
            else:
                if java_type == "String":
                    body = {"type": "string", "description": "Request body, sent as JSON"}
                elif java_type.startswith("List<"):
                    body = schema_for_type(java_type, 1)
                    body["description"] = java_type
                else:
                    body = schema_for_class(java_type, 1)
        key = f"{verb} {path}"
        category = "system" if key in SYSTEM else "sign-in" if key in SIGN_IN else "user"
        tools.append({
            "name": GROUPS[class_name] + "_" + snake(method_name),
            "group": GROUPS[class_name],
            "category": category,
            "description": description or DESCRIPTIONS.get(key, method_name),
            "method": verb,
            "path": path,
            "readOnly": verb == "GET",
            "pathParams": path_params,
            "queryParams": query_params,
            "headerParams": header_params,
            "body": body,
            "multipart": multipart_spec(path) if multipart else None,
            "binary": binary,
            "permitAll": permit_all,
            "resource": class_name + "." + method_name,
        })
    return tools


def multipart_spec(path):
    """The form parts the two upload endpoints read, from their bodies."""
    if path.startswith("/approvalForum/agenda/"):
        return {"filePart": "file", "fields": ["docType"]}
    return {"filePart": "file", "fields": ["appID", "createdBy", "docType", "docDescription"]}


def dedupe_names(tools):
    seen = {}
    for tool in tools:
        if tool["name"] in seen:
            # Same method name twice in a resource (overloads): suffix with the last path segment
            tail = [p for p in tool["path"].split("/") if p and not p.startswith("{")][-1]
            tool["name"] = tool["name"] + "_" + snake(tail)
        seen[tool["name"]] = tool
    names = [t["name"] for t in tools]
    assert len(names) == len(set(names)), "tool names are not unique: " + str([n for n in names if names.count(n) > 1])


def catalog():
    tools = []
    for name in sorted(os.listdir(os.path.join(ROOT, RESOURCES))):
        if name.endswith(".java") and name[:-5] in GROUPS:
            tools += scan(f"{RESOURCES}/{name}", name[:-5])
    tools += scan(f"{PLATFORM}/PlatformTokenResource.java", "PlatformTokenResource")
    tools.sort(key=lambda t: (t["group"], t["path"], t["method"]))
    dedupe_names(tools)
    return {"generated": "mcp/tools/generate-tool-catalog.py", "tools": tools}


def main():
    text = json.dumps(catalog(), indent=2, ensure_ascii=False) + "\n"
    target = os.path.join(ROOT, OUT)
    if "--check" in sys.argv:
        current = open(target, encoding="utf-8").read().replace("\r\n", "\n") if os.path.exists(target) else ""
        if current != text:
            print(f"{OUT} is out of date: run python mcp/tools/generate-tool-catalog.py")
            sys.exit(1)
        print(f"{OUT} is up to date")
        return
    os.makedirs(os.path.dirname(target), exist_ok=True)
    with open(target, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(text)
    tools = json.loads(text)["tools"]
    by_category = {}
    for t in tools:
        by_category[t["category"]] = by_category.get(t["category"], 0) + 1
    print(f"{len(tools)} tools -> {OUT} ({by_category})")


if __name__ == "__main__":
    main()
