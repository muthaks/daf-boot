---
inclusion: always
---

# DAF backend (daf-boot)

## Purpose and scope

daf-boot is the Delegated Authority Forum backend on Java 25 / Spring Boot. It carries the legacy DAF business logic from `reviewmandates/approvalforum` unchanged in behaviour. Read [docs/MODULES.md](../../docs/MODULES.md) for the modules and [docs/MIGRATION.md](../../docs/MIGRATION.md) for the migration rules.

| Module | Responsibility |
| --- | --- |
| `api/` | The DAF API: capture, review and approval workflows, the committee Agenda Builder, documents, reporting, scheduled jobs; JAX-RS resources on Spring, JPA/Hibernate with Envers, Flyway |
| `rules/` | Drools approval rules |
| `docgen/` | Committee document generation |
| `soap/` | SOAP endpoints |
| `support/` | Shared JAX-RS/Spring support |
| `legacy-libs/` | Vendored legacy rules jars installed at validate |

The frontends live in the credit-platform repository. The Agenda Builder UI contract is in that repository's spec folder `specs/agenda-builder/` (`ui-client-contract.md` lists every call it makes to this API). This repository owns the server side of that contract.

## Specs

| Spec | Use |
| --- | --- |
| [Agenda API](../specs/agenda-api/requirements.md) | The Agenda Builder backend: endpoints, rules, invitations, notifications, persistence, jobs |

Each spec has hand-written `requirements.md`, `design.md` and `tasks.md`. Files named `api-*.md` are generated from the source by `api/tools/document-agenda-api.py`. Never edit them by hand. Re-run the script after a change and use `--check` to confirm they match the code.

## Rules

- Endpoint paths, verbs, parameter names, request/response shapes and status codes are a published contract with the Agenda Builder UI and the legacy UI. Change them only together with the consumer, and regenerate the references.
- Behaviour changes go through Flyway migrations (`api/src/main/resources/db/migration`), never manual database edits. Agenda tables are audited by Envers (`A_` tables).
- The legacy `reviewmandates` repository mirrors the agenda service, resource and tests. A behaviour change here is mirrored there in the same piece of work.
- Build and test with JDK 25: `JAVA_HOME=<jdk-25> ./mvnw -o -pl api test`.
- Never put credentials, tokens or personal e-mail addresses in specs or docs. Settings that hold credentials come from the environment.
- Use the Jira key prefix the team works under in commit messages, and respect the repository's hooks.
