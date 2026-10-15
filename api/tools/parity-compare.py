"""Side-by-side parity check: the same requests against the legacy DAF API and daf-api, compared.

    python api/tools/parity-compare.py --legacy http://localhost:40200 --boot http://localhost:40220 \
        --legacy-src <reviewmandates checkout> --samples api/tools/parity-samples.json [--report out.md]

Requests are generated from the legacy JAX-RS resources (every @GET, with path and query parameters filled
from the samples file). GET endpoints with side effects are listed in SIDE_EFFECT_GETS and not called.
Both servers get the same DAF token, issued by the legacy /auth/ssoToken for the samples' WindowsAuthDetail.
Each response is compared on status, media type and body (JSON compared structurally).
"""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

RESOURCES = "approvalforum/approvalforum-web/src/main/java/za/co/rmb/approvalforum/web/resources"
CONTEXT = "/approvalforum/api"

# GETs that change state or reach external systems on purpose: never called by this tool.
SIDE_EFFECT_GETS = {
    "/scheduler/scheduler",          # starts the Quartz scheduler
    "/emailReceived/process",        # processes received approval e-mails
    "/approvalForum/mail/test",      # sends a test e-mail
    "/documents/archive",            # archives a document (stored procedure)
}


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def legacy_gets(legacy_src):
    out = []
    folder = os.path.join(legacy_src, *RESOURCES.split("/"))
    for name in sorted(os.listdir(folder)):
        if not name.endswith(".java"):
            continue
        src = strip_comments(open(os.path.join(folder, name), encoding="utf-8").read())
        cls = re.search(r"@Path\(\s*\"([^\"]*)\"\s*\)[^{;]*?class\s", src, re.S)
        base = cls.group(1) if cls else ""
        for m in re.finditer(r"((?:@\w+(?:\((?:[^()]|\([^()]*\))*\))?\s*)+)public\s+[\w<>?, ]+\s+(\w+)\s*\(((?:[^()]|\([^()]*\))*)\)", src):
            annotations, method, params = m.groups()
            if not re.search(r"@GET\b", annotations):
                continue
            p = re.search(r"@Path\(\s*\"([^\"]*)\"", annotations)
            path = "/" + "/".join(s for s in (base + "/" + (p.group(1) if p else "")).split("/") if s)
            path = re.sub(r"\{\s*(\w+)\s*:[^}]*\}", r"{\1}", path)
            queries = re.findall(r"@QueryParam\(\s*\"([^\"]+)\"\s*\)", params)
            out.append({"resource": name[:-5], "method": method, "path": path, "query": queries})
    return out


def call(base, method, path, token=None, body=None, timeout=60):
    data = None
    headers = {"Accept": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(base + CONTEXT + path, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, resp.headers.get("Content-Type", ""), resp.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers.get("Content-Type", ""), e.read()
    except Exception as e:  # noqa: BLE001 - reported as a result, not raised
        return None, "", repr(e).encode()


def media(content_type):
    return (content_type or "").split(";")[0].strip().lower()


def decode(content_type, raw):
    if media(content_type).endswith("json"):
        try:
            return json.loads(raw.decode("utf-8") or "null")
        except ValueError:
            pass
    return raw.decode("utf-8", errors="replace")


def first_difference(a, b, where="$"):
    if type(a) is not type(b):
        return f"{where}: {type(a).__name__} vs {type(b).__name__}"
    if isinstance(a, dict):
        for key in sorted(set(a) | set(b)):
            if key not in a or key not in b:
                return f"{where}.{key}: present only in {'legacy' if key in a else 'boot'}"
            d = first_difference(a[key], b[key], f"{where}.{key}")
            if d:
                return d
        return None
    if isinstance(a, list):
        if len(a) != len(b):
            return f"{where}: {len(a)} vs {len(b)} items"
        for i, (x, y) in enumerate(zip(a, b)):
            d = first_difference(x, y, f"{where}[{i}]")
            if d:
                return d
        return None
    return None if a == b else f"{where}: {str(a)[:80]!r} vs {str(b)[:80]!r}"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--legacy", required=True)
    ap.add_argument("--boot", required=True)
    ap.add_argument("--legacy-src", required=True)
    ap.add_argument("--samples", required=True)
    ap.add_argument("--report")
    args = ap.parse_args()
    samples = json.load(open(args.samples, encoding="utf-8"))
    values = samples["values"]

    status, ctype, raw = call(args.legacy, "POST", "/auth/ssoToken", body=samples["windowsAuthDetail"])
    if status != 200:
        sys.exit(f"legacy /auth/ssoToken answered {status}: {raw[:300]!r}")
    token = json.loads(raw)["Token"]

    rows, same, differ, skipped = [], 0, 0, 0
    for ep in legacy_gets(args.legacy_src):
        if ep["path"] in SIDE_EFFECT_GETS:
            rows.append(("SKIP", ep, "side effect"))
            skipped += 1
            continue
        missing = [v for v in re.findall(r"\{(\w+)\}", ep["path"]) if v not in values]
        if missing:
            rows.append(("SKIP", ep, "no sample for " + ", ".join(missing)))
            skipped += 1
            continue
        path = re.sub(r"\{(\w+)\}", lambda m: urllib.parse.quote(str(values[m.group(1)])), ep["path"])
        query = {q: values[q] for q in ep["query"] if q in values}
        if query:
            path += "?" + urllib.parse.urlencode(query)
        a = call(args.legacy, "GET", path, token)
        b = call(args.boot, "GET", path, token)
        problems = []
        if a[0] != b[0]:
            problems.append(f"status {a[0]} vs {b[0]}")
        if media(a[1]) != media(b[1]):
            problems.append(f"type {media(a[1]) or '-'} vs {media(b[1]) or '-'}")
        da, db = decode(a[1], a[2]), decode(b[1], b[2])
        # The servlet container's own error page (Tomcat 9 vs 11) is not part of the API: same status is enough
        container_page = a[0] == b[0] and a[0] is not None and a[0] >= 400 and media(a[1]) == media(b[1]) == "text/html"
        diff = None if container_page else first_difference(da, db)
        if diff:
            problems.append(diff)
        ep = dict(ep, called=path, status=a[0])
        if problems:
            rows.append(("DIFF", ep, "; ".join(problems)))
            differ += 1
        else:
            rows.append(("SAME", ep, f"{a[0]} {media(a[1])}"))
            same += 1

    lines = [f"# Parity: {args.legacy} (legacy) vs {args.boot} (daf-api)", "",
             f"| Result | Count |", "| --- | --- |", f"| Same | {same} |", f"| Different | {differ} |",
             f"| Skipped | {skipped} |", "", "| Result | Endpoint | Request | Detail |", "| --- | --- | --- | --- |"]
    for result, ep, detail in rows:
        lines.append(f"| {result} | {ep['resource']}#{ep['method']} | `GET {ep.get('called', ep['path'])}` | {detail} |")
    text = "\n".join(lines) + "\n"
    if args.report:
        open(args.report, "w", encoding="utf-8").write(text)
    print(f"same {same}  different {differ}  skipped {skipped}")
    for result, ep, detail in rows:
        if result != "SAME":
            print(f"{result:4} {ep['resource']}#{ep['method']} GET {ep.get('called', ep['path'])}  {detail}")
    sys.exit(1 if differ else 0)


if __name__ == "__main__":
    main()
