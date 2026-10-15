"""Write parity: the same mutating scenario against the legacy DAF API and daf-api, each on its own copy of
the database, then the responses and the resulting tables compared.

    python api/tools/parity-write.py --legacy http://localhost:40230 --boot http://localhost:40240 \
        --scenario api/tools/parity-scenario.json --samples api/tools/parity-samples.json \
        --db-legacy wc_af_parity_legacy --db-boot wc_af_parity_boot [--sql-server localhost] [--report out.md]

Both databases must be restored from the same backup (identical rows and identity seeds), and neither server
may run its scheduler. Steps run in lock-step: step n on legacy, then on boot. A step is
    {"name", "method", "path", "query": {}, "body": <json>, "merge": {}, "multipart": {"fields": {}, "files": {}},
     "headers": {}, "save": {"var": "$.json.path"}}
with "${var}" substituted from earlier answers (each server keeps its own variables; they are expected equal).
Timestamps that a server generates at "now" differ between the two runs by seconds, so two numbers that are
both epoch milliseconds within an hour of now, or two ISO date-times within an hour of now, count as equal.
Tables are compared with COUNT(*) and CHECKSUM_AGG(BINARY_CHECKSUM(...)) over every column that is not a
date/time type.
"""
import argparse
import datetime as dt
import json
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

CONTEXT = "/approvalforum/api"
NOW_WINDOW_MS = 3600 * 1000
TIME_TYPES = {"date", "datetime", "datetime2", "smalldatetime", "datetimeoffset", "time", "timestamp", "rowversion"}


def multipart_body(spec):
    boundary = "----dafparity7d1f3c"
    crlf = "\r\n"
    parts = []
    for name, value in (spec.get("fields") or {}).items():
        parts.append((f"--{boundary}{crlf}Content-Disposition: form-data; name=\"{name}\"{crlf}{crlf}{value}{crlf}").encode("utf-8"))
    for name, f in (spec.get("files") or {}).items():
        head = (f"--{boundary}{crlf}Content-Disposition: form-data; name=\"{name}\"; filename=\"{f['name']}\"{crlf}"
                f"Content-Type: {f.get('type', 'application/octet-stream')}{crlf}{crlf}")
        parts.append(head.encode("utf-8") + f["content"].encode("utf-8") + crlf.encode("utf-8"))
    parts.append(f"--{boundary}--{crlf}".encode("utf-8"))
    return b"".join(parts), "multipart/form-data; boundary=" + boundary


def call(base, method, path, token, query=None, body=None, multipart=None, extra_headers=None):
    url = base + CONTEXT + path + ("?" + urllib.parse.urlencode(query) if query else "")
    headers = {"Accept": "application/json", "Authorization": "Bearer " + token}
    headers.update(extra_headers or {})
    data = None
    if multipart is not None:
        data, headers["Content-Type"] = multipart_body(multipart)
    elif body is not None:
        data = json.dumps(body).encode("utf-8")
        headers["Content-Type"] = "application/json"
    req = urllib.request.Request(url, data=data, method=method, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, r.headers.get("Content-Type", ""), r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.headers.get("Content-Type", ""), e.read().decode("utf-8", "replace")
    except Exception as e:  # noqa: BLE001 - reported, not raised
        return None, "", repr(e)


def decode(ctype, text):
    if "json" in (ctype or ""):
        try:
            return json.loads(text) if text else None
        except ValueError:
            pass
    return text


def lookup(doc, path):
    cur = doc
    for part in re.findall(r"\.(\w+)|\[(\d+)\]", path[1:] if path.startswith("$") else path):
        key, idx = part
        cur = cur[int(idx)] if idx else cur[key]
    return cur


def substitute(value, variables):
    if isinstance(value, str):
        whole = re.fullmatch(r"\$\{(\w+)\}", value)
        if whole:
            return variables[whole.group(1)]
        return re.sub(r"\$\{(\w+)\}", lambda m: str(variables[m.group(1)]), value)
    if isinstance(value, list):
        return [substitute(v, variables) for v in value]
    if isinstance(value, dict):
        return {k: substitute(v, variables) for k, v in value.items()}
    return value


def near_now(value, now_ms):
    if isinstance(value, (int, float)) and not isinstance(value, bool):
        return abs(value - now_ms) < NOW_WINDOW_MS
    if isinstance(value, str) and re.match(r"\d{4}-\d{2}-\d{2}[T ]\d{2}:\d{2}", value):
        try:
            parsed = dt.datetime.fromisoformat(value.replace("Z", "+00:00")[:19])
            return abs(parsed.timestamp() * 1000 - now_ms) < NOW_WINDOW_MS + 3 * 3600 * 1000
        except ValueError:
            return False
    return False


def difference(a, b, now_ms, where="$"):
    if near_now(a, now_ms) and near_now(b, now_ms):
        return None
    if type(a) is not type(b):
        return f"{where}: {type(a).__name__} vs {type(b).__name__}"
    if isinstance(a, dict):
        for key in sorted(set(a) | set(b)):
            if key not in a or key not in b:
                return f"{where}.{key}: only in {'legacy' if key in a else 'boot'}"
            d = difference(a[key], b[key], now_ms, f"{where}.{key}")
            if d:
                return d
        return None
    if isinstance(a, list):
        if len(a) != len(b):
            return f"{where}: {len(a)} vs {len(b)} items"
        for i, (x, y) in enumerate(zip(a, b)):
            d = difference(x, y, now_ms, f"{where}[{i}]")
            if d:
                return d
        return None
    return None if a == b else f"{where}: {str(a)[:100]!r} vs {str(b)[:100]!r}"


def sql(server, database, query):
    out = subprocess.run(["sqlcmd", "-S", server, "-E", "-d", database, "-h", "-1", "-W", "-s", "\t", "-b",
                          "-Q", "SET NOCOUNT ON; " + query], capture_output=True, text=True, check=True)
    return [line.split("\t") for line in out.stdout.splitlines() if line.strip()]


def table_fingerprints(server, database, ignore=()):
    columns = {}
    for table, column, kind in sql(server, database,
                                   "SELECT c.TABLE_NAME, c.COLUMN_NAME, c.DATA_TYPE FROM INFORMATION_SCHEMA.COLUMNS c "
                                   "JOIN INFORMATION_SCHEMA.TABLES t ON t.TABLE_NAME = c.TABLE_NAME AND t.TABLE_TYPE = 'BASE TABLE' "
                                   "ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION"):
        columns.setdefault(table, [])
        if kind.lower() not in TIME_TYPES and f"{table}.{column}" not in ignore:
            columns[table].append(column)
    result = {}
    for table, cols in columns.items():
        checksum = f"CHECKSUM_AGG(BINARY_CHECKSUM({', '.join('[' + c + ']' for c in cols)}))" if cols else "0"
        count, check = sql(server, database, f"SELECT COUNT(*), ISNULL({checksum}, 0) FROM [{table}]")[0]
        result[table] = (int(count), check)
    return result


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--legacy", required=True)
    ap.add_argument("--boot", required=True)
    ap.add_argument("--scenario", required=True)
    ap.add_argument("--samples", required=True)
    ap.add_argument("--db-legacy", required=True)
    ap.add_argument("--db-boot", required=True)
    ap.add_argument("--sql-server", default="localhost")
    ap.add_argument("--report")
    args = ap.parse_args()
    detail = json.load(open(args.samples, encoding="utf-8"))["windowsAuthDetail"]
    scenario = json.load(open(args.scenario, encoding="utf-8"))

    # Columns that hold the time of the write as a number (e.g. the Envers revision timestamp in epoch ms)
    ignore = set(scenario.get("ignore_columns", []))
    before = (table_fingerprints(args.sql_server, args.db_legacy, ignore), table_fingerprints(args.sql_server, args.db_boot, ignore))
    if before[0] != before[1]:
        sys.exit("the two databases differ before the scenario; restore both from the same backup")

    tokens = {}
    for name, base in (("legacy", args.legacy), ("boot", args.boot)):
        status, ctype, text = call(base, "POST", "/auth/ssoToken", "", body=detail)
        if status != 200:
            sys.exit(f"{name} /auth/ssoToken answered {status}: {text[:200]}")
        tokens[name] = json.loads(text)["Token"]

    variables = {"legacy": dict(scenario.get("variables", {})), "boot": dict(scenario.get("variables", {}))}
    rows, same, differ = [], 0, 0
    for step in scenario["steps"]:
        results = {}
        for name, base in (("legacy", args.legacy), ("boot", args.boot)):
            v = variables[name]
            try:
                path = substitute(step["path"], v)
                query = substitute(step.get("query"), v)
                body = substitute(step.get("body"), v)
                if "merge" in step:
                    body = {**body, **substitute(step["merge"], v)}
                multipart = substitute(step.get("multipart"), v)
                extra_headers = substitute(step.get("headers"), v)
            except KeyError as e:
                results[name] = (None, "", f"missing variable {e}")
                continue
            results[name] = call(base, step["method"], path, tokens[name], query, body, multipart, extra_headers)
            answer = decode(results[name][1], results[name][2])
            for var, json_path in step.get("save", {}).items():
                try:
                    v[var] = lookup(answer, json_path)
                except (KeyError, IndexError, TypeError):
                    pass
        now_ms = time.time() * 1000
        (sa, ca, ta), (sb, cb, tb) = results["legacy"], results["boot"]
        problems = []
        if sa != sb:
            problems.append(f"status {sa} vs {sb}")
        # The servlet container's own error page (Tomcat 9 vs 11) is not part of the API: same status is enough
        container_page = sa == sb and sa is not None and sa >= 500 and "text/html" in ca and "text/html" in cb             and ta.lstrip().lower().startswith("<!doctype html") and tb.lstrip().lower().startswith("<!doctype html")
        d = None if container_page else difference(decode(ca, ta), decode(cb, tb), now_ms)
        if d:
            problems.append(d)
        label = f"{step['method']} {step['path']}"
        if problems:
            differ += 1
            rows.append(("DIFF", step["name"], label, f"{sa}", "; ".join(problems)))
        else:
            same += 1
            rows.append(("SAME", step["name"], label, f"{sa}", ""))

    after = (table_fingerprints(args.sql_server, args.db_legacy, ignore), table_fingerprints(args.sql_server, args.db_boot, ignore))
    changed = sorted(t for t in after[0] if after[0][t] != before[0].get(t))
    table_diffs = sorted(t for t in set(after[0]) | set(after[1]) if after[0].get(t) != after[1].get(t))

    lines = [f"# Write parity: {args.legacy} (legacy) vs {args.boot} (daf-api)", "",
             "| Result | Count |", "| --- | --- |", f"| Same answer | {same} |", f"| Different answer | {differ} |",
             f"| Tables changed by the scenario | {len(changed)} |", f"| Tables that differ afterwards | {len(table_diffs)} |", "",
             "| Result | Step | Request | Status | Detail |", "| --- | --- | --- | --- | --- |"]
    lines += [f"| {r[0]} | {r[1]} | `{r[2]}` | {r[3]} | {r[4]} |" for r in rows]
    lines += ["", "| Table | Legacy rows, checksum | daf-api rows, checksum | Same |", "| --- | --- | --- | --- |"]
    lines += [f"| {t} | {after[0].get(t)} | {after[1].get(t)} | {'yes' if after[0].get(t) == after[1].get(t) else 'NO'} |"
              for t in sorted(set(changed) | set(table_diffs))]
    text = "\n".join(lines) + "\n"
    if args.report:
        open(args.report, "w", encoding="utf-8").write(text)
    print(f"answers same {same} different {differ}; tables changed {len(changed)}, differing afterwards {len(table_diffs)}")
    for r in rows:
        if r[0] != "SAME":
            print(f"DIFF {r[1]}: {r[2]} [{r[3]}] {r[4]}")
    for t in table_diffs:
        print(f"TABLE {t}: legacy {after[0].get(t)} boot {after[1].get(t)}")
    sys.exit(1 if differ or table_diffs else 0)


if __name__ == "__main__":
    main()
