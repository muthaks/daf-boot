#!/usr/bin/env python3
"""Generate the Agenda API reference files in the spec folder (OUT) from the daf-boot source.

The hand-written requirements.md, design.md and tasks.md explain the rules; these generated files
hold the exact contract (endpoints, permissions, DTOs, schema, rules, notifications, settings) so
the backend can be checked or rebuilt like for like. Nothing here calls an API or a database.

    python api/tools/document-agenda-api.py           # rewrite the reference files
    python api/tools/document-agenda-api.py --check   # exit 1 when a reference is out of date

Secret values are never written: settings whose name looks like a credential are masked, and the
run fails if any output looks like a token.
"""
import hashlib
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
JAVA = "api/src/main/java/za/co/rmb/approvalforum"
OUT = ".kiro/specs/agenda-api"

RESOURCE = f"{JAVA}/web/resources/ApprovalForumAgendaResource.java"
SERVICE = f"{JAVA}/service/impl/AgendaServiceImpl.java"
SERVICE_API = f"{JAVA}/service/AgendaService.java"
TEAM = f"{JAVA}/service/impl/AgendaTeamValidation.java"
ACCESS = f"{JAVA}/service/AgendaAccess.java"
PAPERS = f"{JAVA}/service/impl/MeetingPapers.java"
MAIL = f"{JAVA}/service/impl/AgendaMailServiceImpl.java"
MAIL_API = f"{JAVA}/service/AgendaMailService.java"
CALENDAR = f"{JAVA}/service/AgendaCalendarService.java"
GRAPH = f"{JAVA}/web/graph/GraphMeetingClient.java"
REPOSITORY = f"{JAVA}/data/jpa/JPAApprovalForumAgendaRepository.java"
SETTINGS = f"{JAVA}/web/module/ApprovalForumWebResourcesModule.java"
SCHEDULER = f"{JAVA}/web/resources/ApprovalForumSchedulerResource.java"
JOBS = [f"{JAVA}/web/quartz/ProcessAgendaCutOffRequest.java", f"{JAVA}/web/quartz/ProcessAgendaReminderRequest.java"]
DTO_DIR = f"{JAVA}/model/agenda"
ENTITY_DIR = f"{JAVA}/data/entity"
MIGRATIONS = "api/src/main/resources/db/migration"

HEADER = ("\nThis is a generated backend contract reference for the Agenda Builder API in daf-boot. "
          "Literal declarations below are part of the specification; requirements.md and design.md explain them. "
          "It is not evidence of a live service, environment or database.\n\n"
          "Refresh from the repository root with `python api/tools/document-agenda-api.py`; use `--check` to detect drift. "
          "No API, directory or database is accessed.\n\n")

ROLE_NAMES = {"ROLE_SECRETARY": "Secretary", "ROLE_CHAIRPERSON": "Chairperson", "ROLE_APPROVER": "Approver",
              "ROLE_CREDIT_ANALYST": "CreditAnalyst", "reviewerRole()": "ReviewAnalyst"}
ROLE_COLUMNS = ["CreditAnalyst", "ReviewAnalyst", "Approver", "Chairperson", "Secretary"]
SECRET_NAME = re.compile(r"token|secret|password|apikey|api\.key|credential", re.I)


def read(rel):
    with open(os.path.join(ROOT, rel), encoding="utf-8") as handle:
        return handle.read().replace("\r\n", "\n")


def sha(text):
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def source_line(rel):
    return f"Source: `{rel}`. SHA-256: `{sha(read(rel))}`.\n"


def fence(code, lang="java"):
    return f"```{lang}\n{code.rstrip()}\n```\n"


def cell(text):
    return (text or "").replace("|", "\\|").replace("\n", " ").strip()


# ---- Java scanning: split a class body into members without a full parser ----------------------

def class_members(text):
    """Members of the first top-level type: (preamble+declaration text, kind) in source order."""
    i, n = 0, len(text)
    depth = 0
    start_body = None
    members = []
    member_start = None
    state = None  # None, 'line', 'block', 'str', 'chr', 'text'
    while i < n:
        c = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if state == "line":
            if c == "\n":
                state = None
        elif state == "block":
            if c == "*" and nxt == "/":
                state = None
                i += 1
        elif state == "text":
            if text.startswith('"""', i):
                state = None
                i += 2
        elif state == "str":
            if c == "\\":
                i += 1
            elif c == '"':
                state = None
        elif state == "chr":
            if c == "\\":
                i += 1
            elif c == "'":
                state = None
        else:
            if c == "/" and nxt == "/":
                state = "line"
                i += 1
            elif c == "/" and nxt == "*":
                state = "block"
                i += 1
            elif text.startswith('"""', i):
                state = "text"
                i += 2
            elif c == '"':
                state = "str"
            elif c == "'":
                state = "chr"
            elif c == "{":
                depth += 1
                if depth == 1 and start_body is None:
                    start_body = i + 1
                    member_start = i + 1
            elif c == "}":
                depth -= 1
                if depth == 1 and member_start is not None:
                    members.append(text[member_start:i + 1])
                    member_start = i + 1
                elif depth == 0 and start_body is not None:
                    break
            elif c == ";" and depth == 1 and member_start is not None:
                members.append(text[member_start:i + 1])
                member_start = i + 1
        i += 1
    return [m.strip("\n") for m in members if m.strip()]


def strip_comments(text):
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def strip_annotations(text):
    out, i = [], 0
    while i < len(text):
        m = re.match(r"@[\w.]+", text[i:])
        if m and (i == 0 or not (text[i - 1].isalnum() or text[i - 1] == "_")):
            i += m.end()
            j = i
            while j < len(text) and text[j] in " \t\n":
                j += 1
            if j < len(text) and text[j] == "(":
                depth = 0
                while j < len(text):
                    if text[j] == "(":
                        depth += 1
                    elif text[j] == ")":
                        depth -= 1
                        if depth == 0:
                            j += 1
                            break
                    elif text[j] == '"':
                        j += 1
                        while j < len(text) and text[j] != '"':
                            j += 2 if text[j] == "\\" else 1
                    j += 1
                i = j
            continue
        out.append(text[i])
        i += 1
    return "".join(out)


def member_info(member):
    """(kind, name, header) where kind is method, field or type."""
    clean = strip_annotations(strip_comments(member)).strip()
    brace = clean.find("{")
    semi = clean.find(";")
    paren = clean.find("(")
    if re.match(r"(?:(?:public|private|protected|static|final|abstract)\s+)*(?:class|interface|enum|record)\s+(\w+)", clean):
        name = re.match(r"(?:(?:public|private|protected|static|final|abstract)\s+)*(?:class|interface|enum|record)\s+(\w+)", clean).group(1)
        return "type", name, clean[:brace if brace >= 0 else len(clean)]
    if paren >= 0 and (brace < 0 or paren < brace) and (semi < 0 or paren < semi or brace >= 0) \
            and "=" not in clean[:paren]:
        name = re.findall(r"(\w+)\s*$", clean[:paren])
        return "method", (name[0] if name else "?"), clean[:brace if brace >= 0 else semi]
    name = re.findall(r"(\w+)\s*(?:=|;)", clean)
    return "field", (name[0] if name else "?"), clean


def split_top(params):
    parts, depth, cur = [], 0, ""
    for ch in params:
        if ch in "(<":
            depth += 1
        elif ch in ")>":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append(cur)
            cur = ""
        else:
            cur += ch
    if cur.strip():
        parts.append(cur)
    return [p.strip() for p in parts]


def method_params(member):
    clean = strip_comments(member)
    # The parameter list follows the method name, after any annotations.
    header = strip_annotations(clean)
    m = re.search(r"\w+\s*\(", header)
    if not m:
        return ""
    start = m.end()
    depth, j = 1, start
    while j < len(header) and depth:
        if header[j] == "(":
            depth += 1
        elif header[j] == ")":
            depth -= 1
        j += 1
    # Re-read the raw signature to keep parameter annotations.
    raw = clean[clean.find(header[m.start():m.end()].strip()):]
    k = raw.find("(") + 1
    depth, e = 1, k
    while e < len(raw) and depth:
        if raw[e] == "(":
            depth += 1
        elif raw[e] == ")":
            depth -= 1
        e += 1
    return raw[k:e - 1]


def literals(text):
    return [bytes(s, "utf-8").decode("unicode_escape") for s in re.findall(r'"((?:[^"\\]|\\.)*)"', text)]


def annotation_args(member, name):
    m = re.search(r"@" + name + r"\s*\(", member)
    if not m:
        return None
    j, depth = m.end(), 1
    while j < len(member) and depth:
        if member[j] == "(":
            depth += 1
        elif member[j] == ")":
            depth -= 1
        elif member[j] == '"':
            j += 1
            while member[j] != '"':
                j += 2 if member[j] == "\\" else 1
        j += 1
    return member[m.end():j - 1]


def body_of(member):
    k = strip_comments(member)
    return member[member.find("{"):] if "{" in member else ""


def javadoc(member):
    m = re.search(r"/\*\*(.*?)\*/", member, re.S)
    if not m:
        return ""
    lines = [re.sub(r"^\s*\* ?", "", line) for line in m.group(1).split("\n")]
    return " ".join(line.strip() for line in lines if line.strip())


# ---- 1. Endpoint contract and permission matrix -------------------------------------------------

def endpoints():
    text = read(RESOURCE)
    base = literals(annotation_args(text[:text.find("class ApprovalForumAgendaResource")], "Path"))[0]
    result = []
    for member in class_members(text):
        kind, name, _ = member_info(member)
        verb = re.search(r"@(GET|POST|PUT|DELETE|PATCH)\b", member)
        if kind != "method" or not verb:
            continue
        path = literals(annotation_args(member, "Path") or '""')
        api = annotation_args(member, "ApiOperation")
        summary = ""
        if api:
            value = api.split("authorizations")[0]
            summary = "".join(literals(value))
        params = []
        for p in split_top(method_params(member)):
            ann = re.findall(r"@(PathParam|QueryParam|DefaultValue)\s*\(\s*\"([^\"]*)\"\s*\)", p)
            bare = strip_annotations(p).split()
            ptype, pname = (" ".join(bare[:-1]), bare[-1]) if len(bare) >= 2 else (p, "")
            kinds = dict(ann)
            if "PathParam" in kinds:
                params.append(("path", kinds["PathParam"], ptype, kinds.get("DefaultValue")))
            elif "QueryParam" in kinds:
                params.append(("query", kinds["QueryParam"], ptype, kinds.get("DefaultValue")))
            elif "Context" in p:
                continue
            else:
                params.append(("body", pname, ptype, None))
        body = body_of(member)
        guards = []
        for g in re.finditer(r"(forbiddenUnless\w*)\s*\(([^;]*?)\)\s*;", body):
            if body[max(0, g.start() - 7):g.start()].strip() == "return":
                continue  # a conditional refusal (the quorum override), reported as override_roles
            fn, args = g.group(1), split_top(g.group(2))
            roles = [ROLE_NAMES.get(a.strip(), a.strip()) for a in args[1:]] if fn == "forbiddenUnless" else []
            guards.append((fn, literals(args[0])[0] if args and literals(args[0]) else "", roles))
        if "canReassignReviewer(" in body and "forbidden(" in body:
            guards.append(("canReassignReviewer", "reassign reviewer", []))
        if "canCloseCondition(" in body:
            guards.append(("canCloseCondition", "close or waive a condition", []))
        if "if (!hasAnyRole(ROLE_APPROVER))" in body:
            guards = [(fn, what, roles) if fn != "forbiddenUnlessDecidesForItem" else ("decidesForItemUnlessApprover", what, roles)
                      for fn, what, roles in guards]
        conditional = re.search(r"return forbiddenUnless\(\"override quorum\", ([^)]*)\)", body)
        statuses = ["200 OK" if "noContent()" not in body else "204 No Content"]
        if guards:
            statuses.append("403 Forbidden {message}")
        if "Status.BAD_REQUEST" in body:
            statuses.append("400 Bad Request {message}")
        if "catch (IllegalStateException" in body:
            statuses.append("409 Conflict {message}")
        if "draftNotFound" in body or "Status.NOT_FOUND" in body:
            statuses.append("404 Not Found")
        if "INTERNAL_SERVER_ERROR" in body:
            statuses.append("500 with the external error as reason phrase")
        services = list(dict.fromkeys(re.findall(r"agendaService\.(\w+)\(", body)))
        others = list(dict.fromkeys(re.findall(r"(approvalForumListManagementService|casdmRestClient|approvalForumExternalService)\.(\w+)\(", body)))
        result.append({
            "verb": verb.group(1), "path": base + (path[0] if path else ""), "method": name, "summary": summary,
            "consumes": literals(annotation_args(member, "Consumes") or ""),
            "produces": literals(annotation_args(member, "Produces") or "") or (["application/octet-stream"] if "APPLICATION_OCTET_STREAM" in (annotation_args(member, "Produces") or "") else []),
            "transactional": "@Transactional" in member, "params": params, "guards": guards,
            "override_roles": [ROLE_NAMES.get(a.strip(), a.strip()) for a in conditional.group(1).split(",")] if conditional else [],
            "statuses": statuses, "services": services, "others": [f"{a}.{b}" for a, b in others],
            "permissions": "withPermissions(" in body, "source": member,
        })
    return result


def guard_text(guard):
    fn, what, roles = guard
    return {
        "forbiddenUnless": f"role: {' or '.join(roles)}",
        "forbiddenUnlessOwner": "owner: booking analyst or named primary analyst",
        "forbiddenUnlessCanView": "can view the item (owner, assigned reviewer, committee secretariat or chair, approver once submitted)",
        "forbiddenUnlessCanEditParticipants": "owner before Held; committee secretary or chair at any time",
        "forbiddenUnlessCanCapture": "serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role)",
        "forbiddenUnlessCanChangeSchedule": "owner or assigned review analyst, item before approval, meeting Draft or Compiled",
        "canReassignReviewer": "serves this committee as secretariat or chair; item Submitted; meeting Draft or Compiled",
        "forbiddenUnlessDecidesForMeeting": "serves the meeting's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role)",
        "forbiddenUnlessDecidesForCommittee": "serves this committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role)",
        "forbiddenUnlessDecidesForItem": "serves the item's committee as secretary/minute taker/administrator (Secretary role) or chair/alternate chair (Chairperson role)",
        "decidesForItemUnlessApprover": "without the Approver role: serves the item's committee as secretariat (Secretary role) or chair (Chairperson role)",
        "forbiddenUnlessCanViewDocument": "can view the document's item (owner, assigned reviewer, committee secretariat or chair, approver once submitted)",
        "forbiddenUnlessScheduler": "the scheduler's own user only (`af.scheduler.user`, the subject of `af.token`)",
        "canCloseCondition": "serves the condition's committee as secretariat (Secretary role) or chair (Chairperson role)",
    }.get(fn, fn)


def endpoint_contract(eps):
    out = ["# Agenda API endpoint contract\n", HEADER, source_line(RESOURCE)]
    out.append(f"\n{len(eps)} endpoints. Every endpoint first calls `tokenValidation(context)`: a request without a valid DAF bearer token is refused before the method body. "
               "Exceptions the method does not catch (for example `IllegalArgumentException` for an unknown id) are not mapped by an agenda-specific handler.\n")
    out.append("\n| # | Verb | Path | Method | Parameters | Guards | Responses |\n| --- | --- | --- | --- | --- | --- | --- |\n")
    for i, e in enumerate(eps, 1):
        params = "; ".join(f"{k} `{n}`: {cell(t)}" + (f" (default {d})" if d else "") for k, n, t, d in e["params"]) or "—"
        guards = "; ".join(guard_text(g) for g in e["guards"]) or "authenticated"
        if e["override_roles"]:
            guards += f"; a non-blank override reason also needs {' or '.join(e['override_roles'])}"
        out.append(f"| {i} | {e['verb']} | `{e['path']}` | `{e['method']}` | {params} | {cell(guards)} | {', '.join(e['statuses'])} |\n")
    for i, e in enumerate(eps, 1):
        out.append(f"\n## {i}. {e['verb']} {e['path']}\n\n")
        if e["summary"]:
            out.append(f"{e['summary']}\n\n")
        out.append(f"- Method: `{e['method']}`; transactional: {'yes' if e['transactional'] else 'no'}\n")
        if e["consumes"]:
            out.append(f"- Consumes: {', '.join(e['consumes'])}\n")
        if e["produces"]:
            out.append(f"- Produces: {', '.join(e['produces'])}\n")
        if e["services"] or e["others"]:
            out.append(f"- Calls: {', '.join('`AgendaService.' + s + '`' for s in e['services'])}{', ' if e['services'] and e['others'] else ''}{', '.join('`' + o + '`' for o in e['others'])}\n")
        if e["permissions"]:
            out.append("- The returned item carries the caller's capabilities: `canReassignReviewer`, `canChangeSchedule`, `canRequestQuorumException`, `canDecideQuorumException`.\n")
        out.append("\n" + fence(e["source"]))
    return "".join(out)


def permission_matrix(eps):
    text = read(RESOURCE)
    out = ["# Agenda API permission matrix\n", HEADER, source_line(RESOURCE)]
    out.append("\nRoles come from the caller's AD groups (`ApprovalForumAuthResource.getAFGroups`) through the DAF token. "
               "`ReviewAnalyst` is the configured `af.agenda.reviewer.role`. A role only confers oversight in `access()` while it is listed in "
               "`af.agenda.oversight.roles`; the role guards below (`forbiddenUnless`) use `hasAnyRole` directly. "
               "\"Any\" means any authenticated caller. A scope guard narrows a role to the item or committee.\n\n")
    out.append("| Verb | Path | " + " | ".join(ROLE_COLUMNS) + " | Scope |\n| --- | --- | " + " | ".join("---" for _ in ROLE_COLUMNS) + " | --- |\n")
    for e in eps:
        role_guards = [g for g in e["guards"] if g[0] == "forbiddenUnless"]
        scopes = [guard_text(g) for g in e["guards"] if g[0] != "forbiddenUnless"]
        if role_guards:
            allowed = set(role_guards[0][2])
            for g in role_guards[1:]:
                allowed &= set(g[2])
            cols = ["Yes" if r in allowed else "No" for r in ROLE_COLUMNS]
        else:
            cols = ["Any"] * len(ROLE_COLUMNS)
        if e["override_roles"]:
            scopes.append(f"a non-blank override reason also needs {' or '.join(e['override_roles'])}")
        scope = "; ".join(scopes) or "—"
        out.append(f"| {e['verb']} | `{e['path']}` | " + " | ".join(cols) + f" | {cell(scope)} |\n")
    out.append("\n## Guard helpers\n\nExact declarations used by the endpoints above.\n\n")
    for member in class_members(text):
        kind, name, _ = member_info(member)
        if name in ("conflict", "hasAnyRole", "reviewerRole", "access", "isOversightRole", "oversightRoles", "forbidden",
                    "forbiddenUnlessOwner", "forbiddenUnlessCanView", "forbiddenUnlessCanEditParticipants",
                    "forbiddenUnlessCanCapture", "forbiddenUnless", "forbiddenUnlessCanChangeSchedule", "withPermissions",
                    "forbiddenUnlessDecidesForMeeting", "forbiddenUnlessDecidesForCommittee", "forbiddenUnlessDecidesForItem",
                    "forbiddenUnlessCanViewDocument", "forbiddenUnlessScheduler",
                    "draftNotFound", "userName", "dateRange") or (kind == "field" and "ROLE_" in member):
            out.append(fence(member))
    out.append("\n## Item and committee scope rules\n\n" + source_line(SERVICE) + "\n")
    service = read(SERVICE)
    for member in class_members(service):
        kind, name, _ = member_info(member)
        if name in ("sameUser", "isOwner", "hasBeenSubmitted", "servesAsSecretary", "servesAsChair", "canView",
                    "getVisibleItems", "canViewItem", "canEditItem", "canEditParticipants", "canChangeSchedule",
                    "canCaptureOutcome", "canReassignReviewer", "decidesFor", "canRequestQuorumException",
                    "canDecideQuorumException"):
            out.append(fence(member))
    out.append("\n" + source_line(ACCESS) + "\n" + fence(read(ACCESS)))
    return "".join(out)


# ---- 2. DTO contract ----------------------------------------------------------------------------

TRIVIAL = re.compile(r"^\{\s*(return\s+(this\.)?\w+\s*;|(this\.)?\w+\s*=\s*\w+\s*;(\s*return\s+this\s*;)?)\s*\}$", re.S)


def data_contract():
    out = ["# Agenda API data contract\n", HEADER,
           "\nEvery request and response type in `model/agenda`. Fields are listed exactly as declared, with their initialisers. "
           "Accessors that only read or write a field are omitted; any accessor with logic is reproduced. Timestamps are `java.sql.Timestamp` "
           "and serialise as epoch milliseconds unless an annotation says otherwise.\n"]
    for name in sorted(os.listdir(os.path.join(ROOT, DTO_DIR))):
        rel = f"{DTO_DIR}/{name}"
        text = read(rel)
        head = text[:text.find("{")]
        doc = javadoc(head)
        out.append(f"\n## {name[:-5]}\n\n" + source_line(rel))
        if doc:
            out.append(f"\n{doc}\n")
        decl = re.search(r"public\s+(?:final\s+)?class\s+\w+[^{]*", head)
        if decl:
            out.append(f"\nDeclaration: `{' '.join(decl.group(0).split())}`\n")
        fields, logic = [], []
        for member in class_members(text):
            kind, mname, _ = member_info(member)
            if kind == "field":
                fields.append(member.strip())
            elif kind == "method":
                body = body_of(member).strip()
                if not TRIVIAL.match(re.sub(r"\s+", " ", strip_comments(body))):
                    logic.append(member)
            elif kind == "type":
                logic.append(member)
        if fields:
            out.append("\n" + fence("\n".join(fields)))
        for member in logic:
            out.append(fence(member))
    return "".join(out)


# ---- 3. Schema: entity mapping and migrations ---------------------------------------------------

def schema_reference():
    out = ["# Agenda API schema reference\n", HEADER,
           "\nThe agenda tables on SQL Server (`wc_approvalforum`), as mapped by the JPA entities and created by the Flyway migrations. "
           "`V1__baseline.sql` is the pre-agenda DAF baseline and holds no agenda table; agenda history starts at V2. "
           "Entities marked `@Audited` keep Envers history in the matching `A_` table.\n"]
    entities = sorted(n for n in os.listdir(os.path.join(ROOT, ENTITY_DIR)) if re.match(r"ApprovalForumAg[a-z]*[A-Z]\w*Entity\.java$", n) and "Agenda" in n)
    out.append("\n## Entities\n")
    for name in entities:
        rel = f"{ENTITY_DIR}/{name}"
        text = read(rel)
        head = text[:text.find("class ")]
        table = literals(annotation_args(head, "Table") or "")
        out.append(f"\n### {name[:-5]} → `{table[0] if table else '?'}`\n\n" + source_line(rel))
        class_annotations = ", ".join("`@" + a + "`" for a in re.findall(r"@(\w+)", head)) or "none"
        out.append(f"\nClass annotations: {class_annotations}\n\n")
        out.append("| Column | Mapping annotations | Property | Java type |\n| --- | --- | --- | --- |\n")
        for member in class_members(text):
            kind, mname, header = member_info(member)
            if kind != "method" or not re.search(r"@(Column|JoinColumn|Id|ManyToOne|OneToMany|OneToOne|Basic|Lob|Version)\b", member):
                continue
            col = literals((annotation_args(member, "Column") or annotation_args(member, "JoinColumn") or "").split(",")[0])
            anns = [a.strip() for a in re.findall(r"(@\w+(?:\([^)]*\))?)", strip_comments(member.split("{")[0])) if not a.startswith("@Override")]
            rtype = re.sub(r"\b(public|protected|private)\b", "", header).split(mname)[0].strip()
            out.append(f"| {cell(col[0] if col else '')} | {cell(' '.join(anns))} | `{mname}` | `{cell(rtype)}` |\n")
    out.append("\n## Repository queries\n\n" + source_line(REPOSITORY) + "\n" + fence(re.sub(r"^(import|package)[^\n]*\n", "", read(REPOSITORY), flags=re.M).strip()))
    out.append("\n## Migrations\n")
    files = sorted((f for f in os.listdir(os.path.join(ROOT, MIGRATIONS)) if re.match(r"V\d+__.*\.sql$", f)),
                   key=lambda f: int(re.match(r"V(\d+)", f).group(1)))
    for f in files:
        if f.startswith("V1__"):
            continue
        rel = f"{MIGRATIONS}/{f}"
        out.append(f"\n### {f}\n\n" + source_line(rel) + "\n" + fence(read(rel), "sql"))
    return "".join(out)


# ---- 4. Business rules --------------------------------------------------------------------------

def rules_reference():
    out = ["# Agenda API business rules reference\n", HEADER,
           "\nThe service contract and its implementation, in source order, grouped by the section comments in the code. "
           "These declarations are the exact rules behind requirements.md; design.md gives the state machines they implement. "
           "Response mapping (DTO conversion) is included because it decides what each endpoint returns.\n"]
    out.append("\n## Service contract\n\n" + source_line(SERVICE_API) + "\n" + fence(re.sub(r"^(import|package)[^\n]*\n", "", read(SERVICE_API), flags=re.M).strip()))
    text = read(SERVICE)
    out.append("\n## Implementation\n\n" + source_line(SERVICE))
    for member in class_members(text):
        banner = re.search(r"//\s*-{2,}\s*(.*?)\s*-*\s*$", member.split("\n")[0]) if member.lstrip().startswith("//") else None
        for b in re.finditer(r"^\s*//\s*-{2,}\s*(.+?)\s*-*\s*$", member, re.M):
            out.append(f"\n### {b.group(1).strip(' -')}\n\n")
            member = member.replace(b.group(0), "", 1)
        if member.strip():
            out.append(fence(member.strip("\n")))
    for rel, title in ((TEAM, "Team independence"), (PAPERS, "Credit paper selection")):
        out.append(f"\n## {title}\n\n" + source_line(rel) + "\n" + fence(re.sub(r"^(import|package)[^\n]*\n", "", read(rel), flags=re.M).strip()))
    return "".join(out)


# ---- 5. Notifications and calendar invitations --------------------------------------------------

def notification_reference():
    mail = read(MAIL)
    service = read(SERVICE)
    callers = {}
    for member in class_members(service):
        kind, name, _ = member_info(member)
        for call in re.findall(r"agendaMailService\.(\w+)\(", member):
            callers.setdefault(call, []).append(name)
    out = ["# Agenda API notification and invitation reference\n", HEADER,
           "\nE-mails go through the DAF mail service client and are logged in `AGN_AgendaNotification` with status Sent, Failed, Disabled or NoRecipient. "
           "Calendar invitations go to Microsoft Graph as Teams/Outlook events. Subjects carry the `Agenda Builder - ` prefix; outside PROD the "
           "recipients are replaced by `af.mail.test.emails` and the subject is prefixed with `[<af.mail_environment>]`.\n"]
    out.append("\n## E-mails\n\n" + source_line(MAIL) + "\n")
    out.append("| Method | Type logged | Banner | Badge | Recipients | Subject | Paper attached | Sent by |\n| --- | --- | --- | --- | --- | --- | --- | --- |\n")
    for member in class_members(mail):
        kind, name, _ = member_info(member)
        if kind != "method" or not name.startswith("send") or name == "send":
            continue
        call = re.search(r"\bsend\((.*)\);", re.sub(r"\s+", " ", strip_comments(body_of(member))))
        args = split_top(call.group(1)) if call else []
        banner = re.findall(r'banner\("[^"]*",\s*"[^"]*",\s*"([^"]*)",\s*"([^"]*)"', member)
        type_ = ", ".join(literals(args[2])) if len(args) > 2 else ""
        if not type_ and len(args) > 2:
            type_ = args[2]
        subject = args[4] if len(args) > 4 else ""
        out.append(f"| `{name}` | {cell(type_)} | {cell(' / '.join(b[0] for b in banner))} | {cell(' / '.join(b[1] for b in banner))} | "
                   f"`{cell(args[3]) if len(args) > 3 else ''}` | `{cell(subject)}` | {'yes' if len(args) > 6 and args[6].strip() != 'null' else 'no'} | "
                   f"{', '.join('`' + c + '`' for c in callers.get(name, [])) or 'not called'} |\n")
    out.append("\n### Mail service implementation\n\n" + fence(re.sub(r"^(import|package)[^\n]*\n", "", mail, flags=re.M).strip()))
    out.append("\n### Mail service contract\n\n" + source_line(MAIL_API) + "\n" + fence(re.sub(r"^(import|package)[^\n]*\n", "", read(MAIL_API), flags=re.M).strip()))
    style = f"{JAVA}/service/InvitationStyle.java"
    out.append("\n## Calendar invitations\n\nThe invitation content (`itemEvent`, `itemHtml`, `itemSnapshot`, `itemAttendees`, `standingIds`) is in the business rules reference. "
               "The calendar contract and its Graph implementation follow.\n\n" + source_line(CALENDAR) + "\n"
               + fence(re.sub(r"^(import|package)[^\n]*\n", "", read(CALENDAR), flags=re.M).strip()))
    out.append("\n### Invitation HTML styling\n\n" + source_line(style) + "\n" + fence(re.sub(r"^(import|package)[^\n]*\n", "", read(style), flags=re.M).strip()))
    out.append("\n### Microsoft Graph client\n\n" + source_line(GRAPH) + "\n" + fence(re.sub(r"^(import|package)[^\n]*\n", "", read(GRAPH), flags=re.M).strip()))
    return "".join(out)


# ---- 6. Settings and scheduled jobs -------------------------------------------------------------

AGENDA_FILES = [RESOURCE, SERVICE, MAIL, GRAPH, SCHEDULER] + JOBS


def configuration_reference():
    settings = read(SETTINGS)
    defaults = dict(re.findall(r'\.put\(\s*"([^"]+)"\s*,\s*"((?:[^"\\]|\\.)*)"\s*\)', settings))
    env_vars = dict(re.findall(r'String\s+(\w+)\s*=\s*env\("([^"]+)"\)', settings))
    key_env = {}
    mapped = re.findall(r'setProps\(\s*props\s*,\s*"([^"]+)"\s*,\s*(\w+)\s*\)', settings) \
        + re.findall(r'props\.put\(\s*"([^"]+)"\s*,\s*(\w+)\s*\)', settings)
    for key, var in mapped:
        if var in env_vars:
            key_env.setdefault(key, env_vars[var])
    used = {}
    for rel in AGENDA_FILES:
        text = read(rel)
        for member in class_members(text):
            kind, name, _ = member_info(member)
            for key in re.findall(r'(?:afSettings\.get|setting)\(\s*"([^"]+)"\s*\)', member):
                used.setdefault(key, []).append(f"{os.path.basename(rel)[:-5]}.{name}")
    scheduler = read(SCHEDULER)
    agenda_keys = {k for k in used if k.startswith(("af.agenda.", "af.graph.", "af.scheduler.agenda.", "af.mail")) or k in ("af.base", "af.token")}
    for rel in (RESOURCE, SERVICE, MAIL, GRAPH) + tuple(JOBS):
        for key in re.findall(r'(?:afSettings\.get|setting)\(\s*"([^"]+)"\s*\)', read(rel)):
            agenda_keys.add(key)
    for key in re.findall(r'afSettings\.get\("(af\.scheduler\.agenda\.[^"]+)"\)', scheduler):
        agenda_keys.add(key)
    out = ["# Agenda API configuration reference\n", HEADER, source_line(SETTINGS),
           "\nSettings reach the code through the `af_settings` map: a built-in default, replaced by the environment variable where one is mapped. "
           "Values of credentials are never reproduced here.\n\n| Setting | Default | Environment variable | Read by |\n| --- | --- | --- | --- |\n"]
    for key in sorted(agenda_keys):
        default = defaults.get(key)
        if default is None:
            shown = "not set"
        elif SECRET_NAME.search(key):
            shown = "(credential; value not reproduced)" if default else "blank"
        elif "@" in default:
            shown = "(e-mail address set in source; configure per environment)"
        else:
            shown = f"`{default}`" if default else "blank"
        out.append(f"| `{key}` | {cell(shown)} | {('`' + key_env[key] + '`') if key in key_env else 'none'} | {cell(', '.join(sorted(set(used.get(key, ['ApprovalForumSchedulerResource'])))))} |\n")
    block = re.search(r"//Process Agenda Cut-Off.*?Agenda Reminder Cron Scheduled\"\);", scheduler, re.S)
    out.append("\n## Scheduled jobs\n\n" + source_line(SCHEDULER) + "\n")
    if block:
        out.append(fence(block.group(0)))
    for rel in JOBS:
        out.append("\n" + source_line(rel) + "\n" + fence(re.sub(r"^(import|package)[^\n]*\n", "", read(rel), flags=re.M).strip()))
    return "".join(out)


# ---- Output ----------------------------------------------------------------------------------

TOKEN_LIKE = re.compile(r"eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{10,}|(?i:secret|token|password)\"\s*,\s*\"[A-Za-z0-9+/=_.-]{24,}\"")


def build():
    eps = endpoints()
    return {
        "api-endpoint-contract.md": endpoint_contract(eps),
        "api-permission-matrix.md": permission_matrix(eps),
        "api-data-contract.md": data_contract(),
        "api-schema-reference.md": schema_reference(),
        "api-rules-reference.md": rules_reference(),
        "api-notification-reference.md": notification_reference(),
        "api-configuration-reference.md": configuration_reference(),
    }


def main():
    check = "--check" in sys.argv[1:]
    files = build()
    for name, text in files.items():
        if TOKEN_LIKE.search(text):
            sys.exit(f"Refusing to write {name}: it contains a value that looks like a credential")
    stale = []
    for name, text in files.items():
        path = os.path.join(ROOT, OUT, name)
        current = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if current != text:
            stale.append(name)
            if not check:
                os.makedirs(os.path.dirname(path), exist_ok=True)
                with open(path, "w", encoding="utf-8", newline="\n") as handle:
                    handle.write(text)
    eps = sum(1 for _ in endpoints())
    if check:
        for name in stale:
            print(f"Agenda API reference needs refresh: {name}")
        print(f"Checked {len(files)} references; {eps} endpoints.")
        sys.exit(1 if stale else 0)
    print(f"Wrote {len(stale)} of {len(files)} references to {OUT}; {eps} endpoints.")


if __name__ == "__main__":
    main()
