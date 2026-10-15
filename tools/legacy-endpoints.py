"""Write the endpoint list of the legacy JAX-RS resources, the expectation of the endpoint inventory tests.

    python tools/legacy-endpoints.py <reviewmandates checkout or export>

Writes api/src/test/resources/legacy/endpoints.txt (approvalforum-web) and
rules/src/test/resources/legacy/endpoints.txt (approvalforumdrools-web): one line per resource method,
"VERB /path", path relative to the JAX-RS application (/api), parameters as {name}, sorted.
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SETS = {
    "api": "approvalforum/approvalforum-web/src/main/java/za/co/rmb/approvalforum/web/resources",
    "rules": "approvalforumrules/approvalforumdrools/approvalforumdrools-web/src/main/java/za/co/rmb/approvalforumdrools/web/resources",
}
VERBS = ("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")


def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"//[^\n]*", "", src)


def normalise(path):
    path = re.sub(r"\{\s*(\w+)\s*(:[^}]*)?\}", r"{\1}", path)
    return "/" + "/".join(p for p in path.split("/") if p)


def endpoints(folder):
    out = set()
    for name in sorted(os.listdir(folder)):
        if not name.endswith(".java"):
            continue
        src = strip_comments(open(os.path.join(folder, name), encoding="utf-8").read())
        cls = re.search(r"@Path\(\s*\"([^\"]*)\"\s*\)[^{;]*?class\s", src, re.S)
        if not cls:
            continue
        for m in re.finditer(r"((?:@\w+(?:\((?:[^()]|\([^()]*\))*\))?\s*)+)public\s+[\w<>?, ]+\s+\w+\s*\(", src):
            annotations = m.group(1)
            verb = next((v for v in VERBS if re.search(r"@" + v + r"\b", annotations)), None)
            if not verb:
                continue
            p = re.search(r"@Path\(\s*\"([^\"]*)\"", annotations)
            out.add(f"{verb} {normalise(cls.group(1) + '/' + (p.group(1) if p else ''))}")
    return sorted(out)


def main():
    if len(sys.argv) != 2 or not os.path.isdir(sys.argv[1]):
        raise SystemExit(__doc__)
    for module, rel in SETS.items():
        lines = endpoints(os.path.join(sys.argv[1], *rel.split("/")))
        target = os.path.join(ROOT, module, "src", "test", "resources", "legacy", "endpoints.txt")
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "w", encoding="utf-8", newline="\n") as fh:
            fh.write("\n".join(lines) + "\n")
        print(f"{module}: {len(lines)} endpoints -> {os.path.relpath(target, ROOT)}")


if __name__ == "__main__":
    main()
