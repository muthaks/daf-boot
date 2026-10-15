"""Record the legacy rules service's answers as golden data for daf-rules' parity test.

    python rules/tools/rules-golden.py --legacy http://localhost:40200 --token <rules token> \
        --drl rules/src/main/resources/approvalforum/RuleData.drl \
        --out rules/src/test/resources/golden/rules-golden.json.gz

Inputs are generated from the DRL itself: every meetingRule it tests (plus one it does not), WatchList or
not, bank or not, Annual Review or not, every 'fit' it tests, and every country risk-taker group it tests,
alone and in pairs, with facility movement 'Changed' or 'Unchanged'. Each input is POSTed to the legacy
/approvalForumDrools/approvalForum endpoint and the status and JSON answer are stored with it.
RulesGoldenTest replays every input on Drools 10 and expects the same answer.
"""
import argparse
import concurrent.futures
import gzip
import itertools
import json
import re
import urllib.error
import urllib.request

COUNTRIES = {"BOTSWANA": "BWA", "NAMIBIA": "NAM", "ZAMBIA": "ZMB", "NIGERIA": "NGA", "GHANA": "GHA",
             "LESOTHO": "LSO", "SWAZILAND": "SWA", "MOZAMBIQUE": "MOZ", "TANZANIA": "TZA", "MAURITIUS": "MUS",
             "INDIA": "IND", "LONDON": "LDN", "FRIH": "FRH", "FRINT": "FRI", "SA": "ZAF"}
PROPOSALS = ["Annual Review", "New Deal"]


def drl_values(drl):
    s = re.sub(r"/\*.*?\*/", "", drl, flags=re.S)
    s = re.sub(r"//[^\n]*", "", s)
    rules = sorted(set(re.findall(r'"([^"]*)"\.equals\(meetingRule\)', s)))
    fits = sorted(set(re.findall(r'"([^"]*)"\.equalsIgnoreCase\(fit\)', s)))
    groups = sorted({COUNTRIES[c] for c in re.findall(r'(?:isRiskTakerGroupCode|isFacilityMovementChanged)\(ApprovalForumMetadata\.(\w+)\)', s)})
    return rules, fits, groups


def request(meeting, distress="", bank=False, proposal="Annual Review", fit="", groups=(), changed=()):
    return {
        "meetingRule": meeting,
        "proposalType": proposal,
        "distressStatus": distress,
        "bank": bank,
        "fit": fit,
        "riskTakerGroupCode": list(groups),
        "facilityMovement": [{"riskTakerGroupCode": g, "movement": "Changed" if g in changed else "Unchanged"}
                             for g in groups],
    }


def cases(rules, fits, groups):
    for meeting in rules + ["NOT_A_MEETING_RULE"]:
        for distress, bank, proposal, fit in itertools.product(["", "WatchList"], [False, True], PROPOSALS, [""] + fits):
            yield request(meeting, distress, bank, proposal, fit)
        for g in groups:
            for distress, proposal, changed in itertools.product(["", "WatchList"], PROPOSALS, [(), (g,)]):
                yield request(meeting, distress, False, proposal, "", (g,), changed)
        for a, b in itertools.combinations(groups, 2):
            for changed in [(), (a,), (b,), (a, b)]:
                yield request(meeting, "", False, "Annual Review", "", (a, b), changed)


def post(url, token, body):
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(url, data=data, method="POST", headers={
        "Content-Type": "application/json", "Accept": "application/json", "Authorization": "Bearer " + token})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, r.read().decode("utf-8")
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--legacy", required=True)
    ap.add_argument("--token", required=True)
    ap.add_argument("--drl", required=True)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()
    url = args.legacy + "/approvalforumdrools/api/approvalForumDrools/approvalForum"
    rules, fits, groups = drl_values(open(args.drl, encoding="utf-8").read())
    inputs = list(cases(rules, fits, groups))
    print(f"{len(rules)} meeting rules, {len(fits)} fits, {len(groups)} groups -> {len(inputs)} requests")
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        answers = list(pool.map(lambda body: post(url, args.token, body), inputs))
    records = []
    for body, (status, text) in zip(inputs, answers):
        try:
            response = json.loads(text) if text else None
        except ValueError:
            response = text
        records.append({"request": body, "status": status, "response": response})
    with gzip.open(args.out, "wt", encoding="utf-8") as fh:
        json.dump({"source": url, "records": records}, fh, separators=(",", ":"))
    statuses = {}
    for r in records:
        statuses[r["status"]] = statuses.get(r["status"], 0) + 1
    print("statuses:", statuses, " valid:", sum(1 for r in records if isinstance(r["response"], dict) and r["response"].get("valid")))


if __name__ == "__main__":
    main()
