#!/usr/bin/env python3
"""Generate docs/MIGRATION_MAP.md by comparing the legacy DAF source tree with api/.

Every legacy class is matched to a class of the same name under api/src/main/java.
Nothing is hand-maintained: re-run this after each migration step and the table is
the current truth.

    python api/tools/migration-map.py <path-to-reviewmandates>/approvalforum
"""
import os, sys, collections

# Every module of the legacy DAF repository, with the daf-boot module that replaces it.
# DAF is five deployables today (daf-api, daf-ui, daf-rules, daf-socketio, daf-docgen)
# plus a SOAP war and the nginx proxy config; the target keeps that separation.
MODULES = {
    "approvalforum/approvalforum-model":                 "api",
    "approvalforum/approvalforum-jdbc":                  "api",
    "approvalforum/approvalforum-service":               "api",
    "approvalforum/approvalforum-security":              "api",
    "approvalforum/approvalforum-web":                   "api",
    "approvalforum/approvalforum-docgen":                "docgen",
    "approvalforum/approvalforum-soap":                  "soap",
    "approvalforum/approvalforum-socketio":              "socketio",
    "approvalforum/approvalforum-proxy":                 "proxy",
    "approvalforum/approvalforum-ui":                    "web + web-daf",
    "approvalforumrules/approvalforumdrools":            "rules",
    "approvalforumrules/approvalforumrules-client":      "rules",
    "approvalforumrules/approvalforumrules-model":       "rules",
    "approvalforumrules/approvalforumrules-rest-client": "rules",
}
REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
# Classes deliberately not carried over, with the reason shown in the report.
EXCLUDED = {
    # Guice wiring and servlet bootstrap - Spring Boot configuration replaces all of it.
    "ApprovalForumWebResourcesModule": "Guice wiring -> Spring configuration",
    "ApprovalForumServiceModule": "Guice wiring -> Spring configuration",
    "ApprovalForumJdbcModule": "Guice wiring -> Spring configuration",
    "ApprovalForumSecurityModule": "Guice wiring -> Spring configuration",
    "ApprovalForumWebModule": "Guice wiring -> Spring configuration",
    "ApprovalForumGuiceServletContextListener": "Guice bootstrap -> Spring Boot",
    "ApprovalForumJndiDataSourceModule": "JNDI datasource -> spring.datasource",
    "RefDataServiceModule": "Guice wiring -> Spring configuration",
    "SchedulerModule": "Guice wiring -> Spring @EnableScheduling",
    "SwaggerServletContextListener": "Swagger listener -> springdoc-openapi",
    "FlywayMigrationModule": "Guice wiring -> Spring Boot Flyway auto-configuration",
    "FlywayMigrationInitializer": "manual Flyway bootstrap -> Spring Boot Flyway auto-configuration",
    "GitResource": "build-info endpoint -> Spring Boot actuator",
    # Replaced by the Spring Security chain (security/DafTokenAuthenticationFilter +
    # controller/ControllerSupport); the DAF token itself is unchanged.
    "AuthenticationFilter": "JAX-RS container filter -> DafTokenAuthenticationFilter",
    "AuthenticationSsoFilter": "JAX-RS container filter -> DafTokenAuthenticationFilter",
    "AuthorizationFilter": "JAX-RS container filter -> Spring Security authorities",
    "TokenBasedSecurityContext": "JAX-RS SecurityContext -> Spring SecurityContextHolder",
    "SecurityHelper": "JAX-RS token check -> ControllerSupport",
    "JPAInitializer": "Guice-persist bootstrap -> Spring Boot EntityManagerFactory",
    "ApprovalForumPropertiesModule": "Guice wiring -> Spring configuration",
    "JPAApprovalForumEntityModule": "Guice wiring -> Spring configuration",
    "JPAApprovalForumDroolsEntityModule": "Guice wiring -> Spring configuration",
    "GitRepositoryState": "build-info holder -> Spring Boot actuator",
}

# Still shipped by the existing approvalforum-model artifact, which api/ declares as a
# dependency. They become native DTOs later; they are not missing today.
PROVIDED_BY_DEPENDENCY = "approvalforum-model"

# The one genuine gap: Envers needs Hibernate 7 jars that are not in the local cache.
BLOCKED = {}   # hibernate-envers 7.2.4.Final fetched; the audit trail is no longer blocked


SKIP = {"target", "node_modules", "dist", ".git", ".angular", ".idea"}

def java_files(root):
    """Every .java under root, keyed by class name, ignoring build output."""
    out = {}
    for dp, dn, fn in os.walk(root):
        dn[:] = [d for d in dn if d not in SKIP]
        for f in fn:
            if f.endswith(".java"):
                out.setdefault(f[:-5], []).append(os.path.join(dp, f))
    return out

def lines(path):
    try:
        with open(path, encoding="utf-8", errors="replace") as fh:
            return sum(1 for _ in fh)
    except OSError:
        return 0

def main():
    src_root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(REPO, "..", "reviewmandates", "approvalforum")
    target = java_files(REPO)          # any daf-boot module counts as a destination
    rows, totals = [], collections.Counter()
    for mod in MODULES:
        mroot = os.path.join(src_root, mod, "src", "main", "java")
        if not os.path.isdir(mroot):
            continue
        for cls, paths in sorted(java_files(mroot).items()):
            for p in paths:
                rel = os.path.relpath(p, mroot).replace("\\", "/")
                n = lines(p)
                if cls in EXCLUDED:
                    status, dest = "excluded", EXCLUDED[cls]
                elif cls in BLOCKED:
                    status, dest = "blocked", BLOCKED[cls]
                elif mod == PROVIDED_BY_DEPENDENCY:
                    status, dest = "provided", "approvalforum-model artifact (dependency)"
                elif cls in target:
                    status = "migrated"
                    dest = os.path.relpath(target[cls][0], REPO).replace("\\", "/")
                else:
                    status, dest = "PENDING", "-"
                rows.append((mod, rel, n, status, dest))
                totals[status] += 1
                totals[mod + ":" + status] += 1
                if status == "migrated":
                    totals["lines_migrated"] += n
                totals["lines_total"] += n

    out = [ "# DAF migration map", "",
            "Generated by `api/tools/migration-map.py` - do not edit by hand. Re-run it after every",
            "migration step; the table below is regenerated from the two source trees, so a class only",
            "shows as migrated when a class of that name actually exists under `api/src/main/java`.", "",
            "## Summary", "",
            "| Metric | Count |", "| --- | --- |",
            f"| Legacy classes | {totals['migrated'] + totals['PENDING'] + totals['excluded']} |",
            f"| Migrated | {totals['migrated']} |",
            f"| Pending | {totals['PENDING']} |",
            f"| Excluded by design | {totals['excluded']} |",
            f"| Provided by dependency | {totals['provided']} |",
            f"| Blocked | {totals['blocked']} |",
            f"| Legacy lines | {totals['lines_total']} |",
            f"| Lines migrated | {totals['lines_migrated']} |", "",
            "## By module", "",
            "| Module | Migrated | Pending | Excluded | Provided | Blocked |", "| --- | --- | --- | --- | --- | --- |" ]
    for mod in MODULES:
        out.append(f"| `{mod}` -> `{MODULES[mod]}` | {totals[mod+':migrated']} | {totals[mod+':PENDING']} | {totals[mod+':excluded']} | {totals[mod+':provided']} | {totals[mod+':blocked']} |")
    out += ["", "## Every class", "",
            "| Module | Legacy source | Lines | Status | Target in `api/` |",
            "| --- | --- | --- | --- | --- |"]
    for mod, rel, n, status, dest in rows:
        mark = {"migrated": "migrated", "PENDING": "**PENDING**", "excluded": "excluded",
                "provided": "provided", "blocked": "**BLOCKED**"}[status]
        out.append(f"| `{mod}` | `{rel}` | {n} | {mark} | `{dest}` |")
    doc = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "..", "docs", "MIGRATION_MAP.md")
    doc = os.path.normpath(doc)
    with open(doc, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(out) + "\n")
    print(f"{doc}: {totals['migrated']} migrated, {totals['PENDING']} pending, {totals['excluded']} excluded")

if __name__ == "__main__":
    main()
