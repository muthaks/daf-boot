"""Docgen parity: the same report models sent to the legacy docgen and daf-docgen, the PDFs compared.

    python docgen/tools/docgen-parity.py --legacy http://localhost:40204 --boot http://localhost:40244 --token <docgen token>

Each request is built from the model classes in docgen/src/main/java (every String field filled with a value naming
it, two approvals, two chat entries), so every template field is rendered. PDFs are compared byte for byte after
removing what iText writes from the clock (/CreationDate, /ModDate), the random document /ID and the random
font subset prefixes.
"""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request

MODELS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                      "src", "main", "java", "za", "co", "rmb", "approvalforum", "docgen", "model")


def fields(model):
    src = open(os.path.join(MODELS, model + ".java"), encoding="utf-8").read()
    return re.findall(r"private\s+([\w<>]+)\s+(\w+)\s*;", src)


def build(model, tag):
    body = {}
    for kind, name in fields(model):
        if kind == "String":
            body[name] = f"{tag} {name}"
        elif kind == "boolean":
            body[name] = True
        elif kind.startswith("List<"):
            item = kind[5:-1]
            body[name] = [build(item, f"{tag} {name}[{i}]") for i in range(2)]
    return body


def post(base, path, token, body):
    req = urllib.request.Request(base + "/docgen" + path, data=json.dumps(body).encode("utf-8"), method="POST",
                                 headers={"Content-Type": "application/json", "Authorization": "Bearer " + token})
    try:
        with urllib.request.urlopen(req, timeout=120) as r:
            return r.status, r.headers.get("Content-Type", ""), r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers.get("Content-Type", ""), e.read()


def get(base, path, token=None):
    headers = {"Authorization": "Bearer " + token} if token else {}
    req = urllib.request.Request(base + "/docgen" + path, headers=headers)
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, r.headers.get("Content-Type", ""), r.read()
    except urllib.error.HTTPError as e:
        return e.code, e.headers.get("Content-Type", ""), e.read()


def normalise(pdf):
    # iText names embedded font subsets with a random six-letter prefix (ABCDEF+Calibri) on every run
    pdf = re.sub(rb"/[A-Z]{6}\+", rb"/XXXXXX+", pdf)
    pdf = re.sub(rb"/(CreationDate|ModDate)\s*\(D:[^)]*\)", rb"/\1(D:X)", pdf)
    return re.sub(rb"/ID\s*\[\s*<[0-9A-Fa-f]+>\s*<[0-9A-Fa-f]+>\s*\]", rb"/ID [<X><X>]", pdf)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--legacy", required=True)
    ap.add_argument("--boot", required=True)
    ap.add_argument("--token", required=True)
    args = ap.parse_args()
    checks = [
        ("POST /generate", lambda b: post(b, "/generate", args.token, build("ReportOutputModel", "RMB"))),
        ("POST /generateASB", lambda b: post(b, "/generateASB", args.token, build("AshburtonReportOutputModel", "ASB"))),
        ("POST /generatePdfForBroaderAfrica", lambda b: post(b, "/generatePdfForBroaderAfrica", args.token, build("ReportOutputModel", "BA"))),
        ("GET /ping", lambda b: get(b, "/ping")),
        ("POST /generate without token", lambda b: post(b, "/generate", "", build("ReportOutputModel", "RMB"))),
    ]
    failures = 0
    for name, run in checks:
        (sa, ta, ba), (sb, tb, bb) = run(args.legacy), run(args.boot)
        same_type = ta.split(";")[0] == tb.split(";")[0]
        if ta.startswith("application/pdf"):
            same_body = normalise(ba) == normalise(bb)
        elif "json" in ta:
            ja, jb = json.loads(ba or b"null"), json.loads(bb or b"null")
            for j in (ja, jb):
                if isinstance(j, dict) and "timestamp" in j:
                    j["timestamp"] = re.sub(r"\d", "0", j["timestamp"])  # same format, any time
            same_body = ja == jb
            if not same_body:
                print("   legacy", ja, "\n   boot  ", jb)
        else:
            same_body = sa == sb  # error pages: the status is the contract
        ok = sa == sb and same_type and same_body
        failures += not ok
        print(f"{'SAME' if ok else 'DIFF'} {name}: legacy {sa} {ta.split(';')[0]} {len(ba)} bytes, boot {sb} {tb.split(';')[0]} {len(bb)} bytes")
    sys.exit(1 if failures else 0)


if __name__ == "__main__":
    main()
