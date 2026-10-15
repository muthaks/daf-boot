"""Copy the legacy DAF sources into daf-boot, changing only what Java 25 / Jersey 4 / Spring require.

The legacy code keeps its packages, so a diff against reviewmandates shows exactly the rewrites below and
nothing else. Re-run after DAF changes:

    python tools/sync-legacy.py <reviewmandates checkout or export> [api] [rules]

Rewrites (every file):
  javax.{ws.rs,inject,persistence,xml.bind,xml.ws,annotation,servlet,validation,json.bind} -> jakarta.*
  Guice @Inject/@Singleton/@Named/Provider -> jakarta.inject (Spring reads these)
  Guice-persist @Transactional -> Spring @Transactional (both roll back on RuntimeException)
  Jersey 2 repackaged Guava -> Guava
  RESTEasy multipart types -> za.co.rmb.credit.daf.jaxrs.multipart (same API, read by Jersey)
  JAX-RS resources gain @Component @Scope("prototype") (RESTEasy-Guice built one per request) and the
  authentication filter @Component (a singleton provider), so Spring injects them and applies @Transactional
Per target: PATCHES (exact-text changes where a framework API changed), PROVIDER_MODULES (Guice modules kept
as plain classes: their provider methods are the legacy configuration and the Spring config calls them),
EXCLUDE (Guice wiring replaced one-to-one by Spring configuration in za.co.rmb.credit.daf).
"""
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

COMMON_REWRITES = [
    (r"\bjavax\.ws\.rs\b", "jakarta.ws.rs"),
    (r"\bjavax\.inject\b", "jakarta.inject"),
    (r"\bjavax\.persistence\b", "jakarta.persistence"),
    (r"\bjavax\.xml\.bind\b", "jakarta.xml.bind"),
    (r"\bjavax\.xml\.ws\b", "jakarta.xml.ws"),
    (r"\bjavax\.annotation\b", "jakarta.annotation"),
    (r"\bjavax\.servlet\b", "jakarta.servlet"),
    (r"\bjavax\.validation\b", "jakarta.validation"),
    (r"\bjavax\.json\.bind\b", "jakarta.json.bind"),
    (r"\bcom\.google\.inject\.persist\.Transactional\b", "org.springframework.transaction.annotation.Transactional"),
    (r"\bcom\.google\.inject\.name\.Named\b", "jakarta.inject.Named"),
    (r"\bcom\.google\.inject\.Inject\b", "jakarta.inject.Inject"),
    (r"\bcom\.google\.inject\.Singleton\b", "jakarta.inject.Singleton"),
    (r"\bcom\.google\.inject\.Provider\b", "jakarta.inject.Provider"),
    (r"\bjersey\.repackaged\.com\.google\.common\b", "com.google.common"),
    (r"\borg\.jboss\.resteasy\.plugins\.providers\.multipart\.(MultipartFormDataInput|InputPart)\b",
     r"za.co.rmb.credit.daf.jaxrs.multipart.\1"),
]

TARGETS = {
    # daf-api: approvalforum-web war (the DAF API)
    "api": {
        "module": "api",
        "roots": ("za/co/rmb/approvalforum/",),
        "sources": [
            "approvalforum/approvalforum-jdbc/src/main/java",
            "approvalforum/approvalforum-service/src/main/java",
            "approvalforum/approvalforum-security/src/main/java",
            "approvalforum/approvalforum-web/src/main/java",
            # approvalforumrules-client/-model/-rest-client are partly XSD-generated: built jars (legacy-libs)
        ],
        "resources": [
            ("approvalforum/approvalforum-jdbc/src/main/resources/db/migration", "db/migration"),
            ("approvalforum/approvalforum-jdbc/src/main/resources/META-INF/persistence.xml", "legacy/persistence.xml"),
            # Read from the classpath by AuthenticationTokenSettingsImpl (JWT issuer, audience, clock skew, SSO key)
            ("approvalforum/approvalforum-security/src/main/resources/application.properties", "application.properties"),
        ],
        "exclude": {
            "za/co/rmb/approvalforum/data/ApprovalForumPropertiesModule.java",
            "za/co/rmb/approvalforum/data/jpa/JPAApprovalForumEntityModule.java",
            "za/co/rmb/approvalforum/data/jpa/JPAInitializer.java",
            "za/co/rmb/approvalforum/data/flyway/FlywayMigrationModule.java",
            "za/co/rmb/approvalforum/module/ApprovalForumServiceModule.java",
            "za/co/rmb/approvalforum/security/module/ApprovalForumSecurityModule.java",
            "za/co/rmb/approvalforum/web/module/ApprovalForumGuiceServletContextListener.java",
            "za/co/rmb/approvalforum/web/module/ApprovalForumJndiDataSourceModule.java",
            "za/co/rmb/approvalforum/web/module/ApprovalForumWebModule.java",
            "za/co/rmb/approvalforum/web/module/SchedulerModule.java",
            "za/co/rmb/approvalforum/web/module/SwaggerServletContextListener.java",
            # Guice Injector-based; the Spring version (same class name) is in api/src/legacy-overrides
            "za/co/rmb/approvalforum/web/quartz/SchedulerJobFactory.java",
        },
        "provider_modules": {
            "za/co/rmb/approvalforum/web/module/ApprovalForumWebResourcesModule.java",
            "za/co/rmb/approvalforum/web/module/RefDataServiceModule.java",
        },
        "component_dirs": ("za/co/rmb/approvalforum/web/resources/",),
        "component_files": {"za/co/rmb/approvalforum/security/filter/AuthenticationFilter.java"},
        "patches": {
            "za/co/rmb/approvalforum/web/module/RefDataServiceModule.java": [
                ("import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;\n", "import jakarta.ws.rs.client.ClientBuilder;\n"),
                ("return new ResteasyClientBuilder().newClient();", "return ClientBuilder.newClient();"),
            ],
            # Spring 7 removed HttpStatusCodeException.getRawStatusCode(); getStatusCode().value() is the same int
            "za/co/rmb/approvalforum/service/impl/LumosRestServiceImpl.java": [
                ("e.getRawStatusCode()", "e.getStatusCode().value()"),
            ],
            "za/co/rmb/approvalforum/web/graph/GraphMeetingClient.java": [
                ("import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;\n", ""),
                ("if (!blank(proxyHost) && builder instanceof ResteasyClientBuilder) {", "if (!blank(proxyHost)) {"),
                ("((ResteasyClientBuilder) builder).defaultProxy(proxyHost.trim(), port);",
                 "builder.property(org.glassfish.jersey.client.ClientProperties.PROXY_URI, \"http://\" + proxyHost.trim() + \":\" + port);"),
            ],
        },
    },
    # daf-docgen: approvalforum-docgen (Spring Boot; already Spring MVC, only the security configuration changes)
    "docgen": {
        "module": "docgen",
        "roots": ("za/co/rmb/approvalforum/docgen/",),
        "sources": ["approvalforum/approvalforum-docgen/src/main/java"],
        "resources": [
            (f"approvalforum/approvalforum-docgen/src/main/resources/{name}", name)
            for name in ("application.properties", "template.html", "template_ashburton.html", "header.html",
                         "footer.html", "style.css", "calibri.ttf", "rmb.png", "RMB_Horizontal_Identity_RGB_Black.png",
                         "FNB_Horizontal_Identity_RGB.png")
        ],
        "exclude": {
            # WebSecurityConfigurerAdapter was removed in Spring Security 6: the same rules as a SecurityFilterChain,
            # same class name, are in docgen/src/legacy-overrides
            "za/co/rmb/approvalforum/docgen/config/SecurityConfig.java",
            # Springfox (Swagger 2 UI) does not run on Spring Boot 3+
            "za/co/rmb/approvalforum/docgen/config/SwaggerConfig.java",
        },
        "provider_modules": set(),
        "component_dirs": (),
        "component_files": set(),
        "patches": {},
    },
    # daf-soap: approvalforum-soap war (Spring WS; external create/cancel requests forwarded to the API)
    "soap": {
        "module": "soap",
        "roots": ("za/co/rmb/approvalforumsoap/",),
        "sources": ["approvalforum/approvalforum-soap/src/main/java"],
        "resources": [
            ("approvalforum/approvalforum-soap/src/main/resources/application.properties", "application.properties"),
            ("approvalforum/approvalforum-soap/src/main/resources/logback.xml", "logback.xml"),
            ("approvalforum/approvalforum-soap/src/main/resources/xsd/approvalforumsoap.xsd", "xsd/approvalforumsoap.xsd"),
        ],
        "exclude": {
            # XWSS (XwsSecurityInterceptor) does not exist for jakarta Spring WS; the same configuration with the
            # UsernameToken validated by WSS4J, same class name, is in soap/src/legacy-overrides
            "za/co/rmb/approvalforumsoap/ws/WebServiceConfig.java",
            # war bootstrap (SpringBootServletInitializer): the service runs as an executable jar
            "za/co/rmb/approvalforumsoap/ServletInitializer.java",
        },
        "provider_modules": set(),
        "component_dirs": (),
        "component_files": set(),
        "patches": {
            # Spring 6.2 deprecated UriComponentsBuilder.fromHttpUrl in favour of fromUriString and Spring 7 removed
            # it; both parse the http(s) AF_BASE URL into the same components
            "za/co/rmb/approvalforumsoap/ws/ApprovalForumExternalEndpoint.java": [
                ("UriComponentsBuilder.fromHttpUrl(", "UriComponentsBuilder.fromUriString("),
            ],
        },
    },
    # daf-rules: approvalforumdrools-web war (the rules service)
    "rules": {
        "module": "rules",
        "roots": ("za/co/rmb/approvalforumdrools/",),
        "sources": [
            "approvalforumrules/approvalforumdrools/approvalforumdrools-jdbc/src/main/java",
            "approvalforumrules/approvalforumdrools/approvalforumdrools-model/src/main/java",
            "approvalforumrules/approvalforumdrools/approvalforumdrools-security/src/main/java",
            "approvalforumrules/approvalforumdrools/approvalforumdrools-service/src/main/java",
            "approvalforumrules/approvalforumdrools/approvalforumdrools-web/src/main/java",
        ],
        "resources": [
            ("approvalforumrules/approvalforumdrools/approvalforumdrools-jdbc/src/main/resources/META-INF/persistence.xml", "legacy/persistence.xml"),
            ("approvalforumrules/approvalforumdrools/approvalforumdrools-security/src/main/resources/application.properties", "application.properties"),
            # The rules the service loads come from R_Rules; this is the file they were loaded from (tests use it)
            ("approvalforumrules/approvalforumdrools/approvalforumdrools-service/src/main/resources/approvalforum/RuleData.drl", "approvalforum/RuleData.drl"),
        ],
        "exclude": {
            "za/co/rmb/approvalforumdrools/data/jpa/JPAApprovalForumDroolsEntityModule.java",
            "za/co/rmb/approvalforumdrools/data/jpa/JPAInitializer.java",
            "za/co/rmb/approvalforumdrools/module/ApprovalForumDroolsEngineModule.java",
            "za/co/rmb/approvalforumdrools/module/ApprovalForumDroolsServiceModule.java",
            "za/co/rmb/approvalforumdrools/security/module/ApprovalForumDroolsSecurityModule.java",
            "za/co/rmb/approvalforumdrools/web/module/ApprovalForumDroolsGuiceServletContextListener.java",
            "za/co/rmb/approvalforumdrools/web/module/ApprovalForumDroolsJndiDataSourceModule.java",
            "za/co/rmb/approvalforumdrools/web/module/ApprovalForumDroolsWebModule.java",
            "za/co/rmb/approvalforumdrools/web/module/ApprovalForumDroolsWebResourcesModule.java",
            "za/co/rmb/approvalforumdrools/web/module/SwaggerServletContextListener.java",
        },
        "provider_modules": set(),
        "component_dirs": ("za/co/rmb/approvalforumdrools/web/resources/",),
        "component_files": {"za/co/rmb/approvalforumdrools/security/filter/AuthenticationFilter.java"},
        "patches": {
            # Drools 7's org.drools.core.spi.KnowledgeHelper is internal API and was moved in Drools 8. The rules
            # call displayRuleTriggered(drools); both engines pass a org.kie.api.runtime.rule.RuleContext (public
            # API since Drools 6), and the method only logs drools.getRule().getName().
            "za/co/rmb/approvalforumdrools/engine/ApprovalForumDroolsEngineUtil.java": [
                ("import org.drools.core.spi.KnowledgeHelper;\n", "import org.kie.api.runtime.rule.RuleContext;\n"),
                ("public static void displayRuleTriggered(final KnowledgeHelper drools){",
                 "public static void displayRuleTriggered(final RuleContext drools){"),
            ],
        },
    },
}


def strip_guice_module(rel, text):
    """Drop 'extends AbstractModule', configure() and @Provides; keep every provider method as written."""
    text, n = re.subn(r"\s+extends\s+AbstractModule\b", "", text, count=1)
    if n != 1:
        raise SystemExit(f"{rel}: not a Guice AbstractModule")
    m = re.search(r"\n\s*@Override\s*\n\s*protected void configure\(\)\s*\{", text)
    if not m:
        raise SystemExit(f"{rel}: configure() not found")
    depth, i = 0, m.end() - 1
    while True:
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                break
        i += 1
    text = text[:m.start()] + text[i + 1:]
    text = re.sub(r"^\s*@Provides\s*\n", "", text, flags=re.M)
    text = re.sub(r"^import com\.google\.inject\.(AbstractModule|Provides|name\.Names);\n", "", text, flags=re.M)
    return text


COMPONENT = "@org.springframework.stereotype.Component\n"
PROTOTYPE = "@org.springframework.context.annotation.Scope(\"prototype\")\n"


def component_annotations(target, rel):
    """@Component for the resources (prototype: RESTEasy-Guice built one per request) and the filter (singleton)."""
    if rel in target["component_files"]:
        return COMPONENT
    for d in target["component_dirs"]:
        if rel.startswith(d) and "/" not in rel[len(d):]:
            return COMPONENT + PROTOTYPE
    return None


def rewrite(target, rel, text):
    if rel in target["provider_modules"]:
        text = strip_guice_module(rel, text)
    for old, new in target["patches"].get(rel, []):
        if old not in text:
            raise SystemExit(f"patch target not found in {rel}: {old!r}")
        text = text.replace(old, new)
    for pattern, repl in COMMON_REWRITES:
        text = re.sub(pattern, repl, text)
    annotations = component_annotations(target, rel)
    if annotations:
        text, n = re.subn(r"^(public\s+class\s)", lambda m: annotations + m.group(1), text, count=1, flags=re.M)
        if n != 1:
            raise SystemExit(f"no public class declaration in {rel}")
    return text


def sync(legacy, name):
    target = TARGETS[name]
    java_out = os.path.join(ROOT, target["module"], "src", "main", "java")
    res_out = os.path.join(ROOT, target["module"], "src", "main", "resources")
    # Check every input before removing the previous copy
    for src in target["sources"] + [r[0] for r in target["resources"]]:
        if not os.path.exists(os.path.join(legacy, *src.split("/"))):
            raise SystemExit(f"missing legacy path: {os.path.join(legacy, *src.split('/'))}")
    for root in target["roots"]:
        shutil.rmtree(os.path.join(java_out, *root.strip("/").split("/")), ignore_errors=True)
    copied = 0
    for src in target["sources"]:
        base = os.path.join(legacy, *src.split("/"))
        if not os.path.isdir(base):
            raise SystemExit(f"missing legacy source root: {base}")
        for dp, _, files in os.walk(base):
            for f in files:
                if not f.endswith(".java"):
                    continue
                rel = os.path.relpath(os.path.join(dp, f), base).replace(os.sep, "/")
                if rel in target["exclude"]:
                    continue
                if not rel.startswith(target["roots"]):
                    raise SystemExit(f"unexpected package outside {target['roots']}: {rel}")
                text = open(os.path.join(dp, f), encoding="utf-8").read()
                out = os.path.join(java_out, *rel.split("/"))
                os.makedirs(os.path.dirname(out), exist_ok=True)
                with open(out, "w", encoding="utf-8", newline="") as fh:
                    fh.write(rewrite(target, rel, text))
                copied += 1
    for src, dest in target["resources"]:
        source = os.path.join(legacy, *src.split("/"))
        dst = os.path.join(res_out, *dest.split("/"))
        if os.path.isdir(source):
            shutil.rmtree(dst, ignore_errors=True)
            shutil.copytree(source, dst)
        else:
            os.makedirs(os.path.dirname(dst), exist_ok=True)
            shutil.copyfile(source, dst)
    print(f"{name}: copied {copied} legacy classes; excluded {len(target['exclude'])}; "
          f"provider modules {len(target['provider_modules'])}; patched {len(target['patches'])} file(s)")


def main():
    if len(sys.argv) < 2 or sys.argv[1].startswith("-") or not os.path.isdir(sys.argv[1]):
        raise SystemExit(__doc__)
    names = sys.argv[2:] or list(TARGETS)
    unknown = [n for n in names if n not in TARGETS]
    if unknown:
        raise SystemExit(f"unknown target(s): {', '.join(unknown)}; known: {', '.join(TARGETS)}")
    for name in names:
        sync(sys.argv[1], name)


if __name__ == "__main__":
    main()
