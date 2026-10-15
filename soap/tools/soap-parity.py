#!/usr/bin/env python3
"""SOAP parity: the same SOAP requests to the legacy and the new service, both forwarding to one recording stub.

Each case sets what the stub answers (status, content type, body), sends the SOAP request to both services and
compares the SOAP responses (status and body, with fault strings that name the stack shown side by side) and the
request each service forwarded to the stub (method, path, query, the headers the endpoint sets, body).

Usage: soap-parity.py <legacy soap base> <new soap base> <stub port>
  e.g. soap-parity.py http://localhost:40247/approvalforum-soap http://localhost:40246/approvalforum-soap 40249
Both services run with AF_BASE=http://<host>:<stub port>/approvalforum/api/ and the same AF_TOKEN / AF_SOAP_USER /
AF_SOAP_PASS.
"""
import json
import re
import sys
import threading
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from xml.etree import ElementTree

NS = "http://credit.rmb.co.za/approvalforum/external"
WSSE = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
PW_TEXT = "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText"

stub_answer = {"status": 200, "type": None, "body": b""}
recorded = []


class Stub(BaseHTTPRequestHandler):
    def do_POST(self):
        if (self.headers.get("Transfer-Encoding") or "").lower() == "chunked":
            body = b""
            while True:
                size = int(self.rfile.readline().strip().split(b";")[0], 16)
                if size == 0:
                    self.rfile.readline()
                    break
                body += self.rfile.read(size)
                self.rfile.readline()
        else:
            body = self.rfile.read(int(self.headers.get("Content-Length") or 0))
        recorded.append({
            "method": "POST",
            "path": self.path,
            "content-type": self.headers.get("Content-Type"),
            "accept": self.headers.get("Accept"),
            "authorization": self.headers.get("Authorization"),
            "content-length": self.headers.get("Content-Length"),
            "transfer-encoding": self.headers.get("Transfer-Encoding"),
            "body": body.decode("utf-8", "replace"),
        })
        self.send_response(stub_answer["status"])
        if stub_answer["type"]:
            self.send_header("Content-Type", stub_answer["type"])
        self.send_header("Content-Length", str(len(stub_answer["body"])))
        self.end_headers()
        self.wfile.write(stub_answer["body"])

    def log_message(self, *args):
        pass


def envelope(op, app_id, tenant, user="user", password="pass", security=True):
    sec = ""
    if security:
        sec = (f'<soapenv:Header><wsse:Security xmlns:wsse="{WSSE}" soapenv:mustUnderstand="1">'
               f'<wsse:UsernameToken><wsse:Username>{user}</wsse:Username>'
               f'<wsse:Password Type="{PW_TEXT}">{password}</wsse:Password>'
               f'</wsse:UsernameToken></wsse:Security></soapenv:Header>')
    tenant_el = f"<ext:tenantId>{tenant}</ext:tenantId>" if tenant is not None else ""
    return (f'<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ext="{NS}">'
            f'{sec}<soapenv:Body><ext:{op}><ext:applicationId>{app_id}</ext:applicationId>{tenant_el}'
            f'</ext:{op}></soapenv:Body></soapenv:Envelope>').encode()


def call(url, data, method="POST"):
    req = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        req.add_header("Content-Type", "text/xml;charset=UTF-8")
        req.add_header("SOAPAction", '""')
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            return r.status, r.headers.get("Content-Type"), r.read().decode("utf-8", "replace")
    except urllib.error.HTTPError as e:
        return e.code, e.headers.get("Content-Type"), e.read().decode("utf-8", "replace")


def normalise(body):
    # the WSDL's soap:address names the port it was requested on
    body = re.sub(r"localhost:\d+", "localhost:PORT", body)
    # XML is compared in canonical form (C14N 2.0): the JDK 8 serializer wrote attributes sorted, later JDKs keep
    # the document order; C14N sorts them and leaves everything else (text, whitespace, namespaces) as it is
    if body.startswith("<"):
        try:
            return ElementTree.canonicalize(re.sub(r"^<\?xml[^>]*\?>", "", body))
        except ElementTree.ParseError:
            pass
    return body


def fault_code(body):
    m = re.search(r"<faultcode>([^<]*)</faultcode>", body)
    return m.group(1) if m else None


def fault_string(body):
    m = re.search(r"<faultstring[^>]*>(.*?)</faultstring>", body, re.S)
    return m.group(1) if m else ""


def fault_text_only(a, b):
    """Both answered the same SOAP fault (status, fault code, forwarded request) with a different fault string."""
    return (a["status"] == b["status"] and a["content-type"] == b["content-type"] and a["forwarded"] == b["forwarded"]
            and fault_code(a["body"]) is not None and fault_code(a["body"]) == fault_code(b["body"])
            and re.sub(r"<faultstring.*?</faultstring>", "", a["body"], flags=re.S)
            == re.sub(r"<faultstring.*?</faultstring>", "", b["body"], flags=re.S))


def main():
    legacy, new, stub_port = sys.argv[1].rstrip("/"), sys.argv[2].rstrip("/"), int(sys.argv[3])
    server = ThreadingHTTPServer(("0.0.0.0", stub_port), Stub)
    threading.Thread(target=server.serve_forever, daemon=True).start()

    ws = "/approvalforumexternal/ws"
    cases = [("GET wsdl", "GET", ws + "/approvalForumExternal.wsdl", None, None),
             ("GET xsd", "GET", ws + "/approvalForumExternalSchema.xsd", None, None)]
    stub_modes = {
        "200 empty": (200, None, b""),
        "200 json": (200, "application/json", b'{"ok":true}'),
        "500": (500, "text/plain", b"boom"),
        # what the API's external create/cancel answer: Response.ok() / Response.serverError(), no body
        "500 empty": (500, None, b""),
        "401": (401, None, b""),
    }
    for op in ("createExternalRequest", "cancelExternalRequest"):
        for tenant in (None, 1, 2, 3):
            for mode in stub_modes:
                cases.append((f"{op} tenant={tenant} stub={mode}", "POST", ws, envelope(op, 4711, tenant), mode))
    cases += [
        ("create wrong password", "POST", ws, envelope("createExternalRequest", 4711, 1, password="nope"), "200 empty"),
        ("create no security header", "POST", ws, envelope("createExternalRequest", 4711, 1, security=False), "200 empty"),
        ("create invalid payload", "POST", ws,
         envelope("createExternalRequest", "abc", 1), "200 empty"),
        ("unknown operation", "POST", ws, envelope("unknownRequest", 1, None), "200 empty"),
    ]

    same = text = diff = 0
    for name, method, path, data, mode in cases:
        results = []
        for base in (legacy, new):
            if mode:
                stub_answer["status"], stub_answer["type"], stub_answer["body"] = stub_modes[mode]
            recorded.clear()
            status, ctype, body = call(base + path, data, method)
            results.append({"status": status, "content-type": ctype, "body": normalise(body),
                            "forwarded": list(recorded)})
        a, b = results
        if a == b:
            same += 1
            print(f"SAME  {name}  ({a['status']}, forwarded {len(a['forwarded'])})")
        elif fault_text_only(a, b):
            text += 1
            print(f"TEXT  {name}  (same status {a['status']}, fault code {fault_code(a['body'])} and forwarded request;"
                  f" fault string differs)\n     legacy: {fault_string(a['body'])[:300]}\n"
                  f"     new:    {fault_string(b['body'])[:300]}")
        else:
            diff += 1
            print(f"DIFF  {name}")
            for key in ("status", "content-type", "forwarded", "body"):
                if a[key] != b[key]:
                    print(f"   {key}:\n     legacy: {json.dumps(a[key])[:1500]}\n     new:    {json.dumps(b[key])[:1500]}")
    print(f"\n{same} same, {text} fault string only, {diff} different of {len(cases)}")
    server.shutdown()


if __name__ == "__main__":
    main()
