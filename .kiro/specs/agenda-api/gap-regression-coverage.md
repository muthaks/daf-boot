# Agenda gap regression coverage

These tests assert the intended protected behaviour for the nine gaps raised in the review. They are ordinary JUnit tests: a missing protection fails the build. They do not regard the formerly permissive behaviour as the expected result, skip known gaps, or wrap a failed security assertion as an expected success.

## Gap-to-test map

| Gap | Assertions |
| --- | --- |
| G1: document access | An unrelated authenticated user receives 403 before document metadata/bytes are read. An authorized reader receives the document. The real access predicate follows the document's owning item, allows its owner, denies an unrelated user and denies an unknown document. |
| G2: close/waive conditions | Both Closed and Waived are denied without condition authorization and reach the service when authorized. Real condition authorization requires the actual committee and role. Both transitions refuse a Closed meeting without saving; successful closure on Held meetings retains actor/time audit fields. |
| G3: meeting item disclosure | The resource calls the caller-scoped meeting service rather than the unfiltered method. An unrelated reader receives no counterparty, analyst/reviewer identity, documents or post-meeting record through item details. Busy-slot placeholders are permitted. The item's owner still receives full item details. |
| G5: approval/rejection mismatch | The same Secretary and Chairperson group mappings can approve and reject when serving the committee, and are denied both actions for a foreign committee. |
| G6: committee scope | A 16-action matrix checks both Secretary and Chairperson mappings with allowed and denied scope: committee update/deactivation; meeting creation/update; invitation send/update; held/close/reopen/cancel/reinstate; meeting-record attachment; item reopen; post-meeting unlock; item approve/reject. Denials must not invoke a mutation or send an invitation. Real committee/meeting predicates also reject an unrelated chair and a caller without the required role. |
| G9: forged sign-off | Both analyst and chair sign-off names become the actual caller; timestamps fall within server execution time despite a forged historical request time. An existing signature and its timestamp cannot be replaced by another caller's payload. |
| G17: embedded credentials/recipient | Every token, secret, password and API-key default is empty and the test mailbox is externally configured. Failure messages identify keys only and never print credential or personal recipient values. |
| G21: scheduler | For deadlines on each weekday, the default cut-off schedule supplies a run within an hour rather than waiting until Thursday. Separate environment overrides for cut-off and reminder cron are exercised in a child JVM. Blank overrides preserve valid defaults. |
| G22: reviewer mail | Real submission-mail construction resolves non-email reviewer and secretary IDs through mocked directory records; the captured mail envelope contains the resolved RMB addresses and deduplicated analyst address, not raw usernames. |

## Test ownership

- [AgendaSecurityRegressionTest](../../../api/src/test/java/za/co/rmb/approvalforum/web/resources/AgendaSecurityRegressionTest.java): resource authorization, delegation and allowed/denied action matrix.
- [AgendaIntegrityRegressionTest](../../../api/src/test/java/za/co/rmb/approvalforum/service/impl/AgendaIntegrityRegressionTest.java): actual service membership, visibility, condition and signature rules.
- [AgendaRecipientRegressionTest](../../../api/src/test/java/za/co/rmb/approvalforum/service/impl/AgendaRecipientRegressionTest.java): actual recipient construction with mocked directory and mail clients.
- [AgendaSettingsRegressionTest](../../../api/src/test/java/za/co/rmb/approvalforum/web/module/AgendaSettingsRegressionTest.java): defaults and isolated environment configuration. The child process removes inherited `AF_*` variables and outputs only the two test cron settings.

No test contacts business APIs, AD, a database, Graph or a mail server. All repository and integration clients are mocks. Fictional addresses use `name.surname@rmb.co.za`. Generated Maven reports/logs stay under ignored `api/target/`; authored tests and this coverage map belong in Git.

From the daf-boot repository root with JDK 25:

```text
./mvnw -o -pl api -Dtest=AgendaSecurityRegressionTest,AgendaIntegrityRegressionTest,AgendaRecipientRegressionTest,AgendaSettingsRegressionTest test
./mvnw -o -pl api test
```

Passing unit tests establish these tested behaviours in the working tree. They do not establish production deployment, live authorization through the gateway, database integration, credential rotation, or parity of the separate legacy mirror. Gaps outside the nine listed above retain their own task status.

## Executed evidence

On 2026-10-31, all 93 regression cases passed: 71 resource cases, 12 service integrity cases, nine settings/scheduler cases and one recipient-construction case. The full API suite passed 421 tests with zero failures, errors or skips. After broadening the credential-key check to include password and API-key suffixes, the nine settings cases were rerun and passed. No production source was changed as part of adding these test files; they exercise the security fixes already present in the shared working tree.
