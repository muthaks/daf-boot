# api

Reserved for the DAF backend migration to Java 25 / Spring Boot 4. Empty by design today: the Committee Agendas remote in `web/` calls the existing `approvalforum` API through the Credit API gateway (`/daf`), so nothing here is needed for the UI to ship.

| When it starts                       | Constraint                                                                                 |
| ------------------------------------ | ------------------------------------------------------------------------------------------ |
| Layout                               | Same as `ccirm/api`: Spring Boot, `za.co.rmb.credit.daf`, Dockerfile, `/actuator/health`   |
| Contract                             | Keep `/approvalForum/agenda/...` and `/auth/...` shapes so `web/` needs no change           |
| Auth                                 | Accept auth-service tokens for system code `DAF` (see `docs/AUTHENTICATION.md` §1)          |
| Build                                | Add a Maven API step and API image to `cat-utils` `.teamcity/DafBoot` when the module exists |
