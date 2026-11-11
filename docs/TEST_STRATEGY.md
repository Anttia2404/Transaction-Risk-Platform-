# Test Strategy

Test plan only; no tests are claimed executed. Use JUnit 5/Mockito for unit tests and PostgreSQL Testcontainers for integration/constraint/transaction behavior; do not substitute H2 for PostgreSQL correctness.

## Unit tests
- Validation: zero/negative amount, >4 decimals, invalid currency/time rejected; valid accepted.
- Rules: each reason/weight, deterministic result, cap at 100, threshold equality (`score==70` alerts), 69 does not.
- State machine: allowed transitions succeed; invalid transitions reject without mutation.
- Hash canonicalization: field ordering stable; semantic payload change changes hash.

## Integration with Testcontainers
- Flyway migrates empty PostgreSQL; repository CRUD and constraints work.
- Below-threshold transaction creates no alert; threshold/above creates one with immutable score snapshot.
- Force alert/audit insert failure; transaction, idempotency and related rows all rollback.
- Unique source/external ID and source/key enforced; one-to-one alert/case enforced.
- Pagination/filter results stable and correctly scoped.

## Security tests
- Login success; wrong password/unknown user generic 401.
- Missing, malformed, expired, wrong-signature JWT → 401; ANALYST admin-only request → 403.
- Analyst JWT alone cannot authenticate simulator intake.
- Verify response/log capture excludes credentials/tokens.

## Idempotency and concurrency
- Same key/same payload sequential: one transaction/alert/audit set and replayed response.
- Same key/different payload: 409, original unchanged.
- Barrier-synchronized same-key concurrent requests: one commit, both consistent response.
- Different keys/same external ID concurrently: one success, one documented 409.
- Simulated response loss after commit + retry: replay with no duplicate effects.
- Different sources can reuse key/external ID if source-scoped uniqueness is approved.

## Optimistic locking and rollback
- Two clients read same version; first update increments; second gets 409, cannot overwrite; only successful action has audit.
- Inject failure after transaction insert or during audit insert; all in-scope writes rollback.
- Case duplicate race produces one case.

## Proposed rule set for demo (needs approval)
| Rule | Condition | Points |
|---|---|---:|
| HIGH_AMOUNT | amount >= 5,000 | 35 |
| VERY_HIGH_AMOUNT | amount >= 10,000; replaces HIGH_AMOUNT, never stacks | 50 |
| HIGH_RISK_COUNTRY | country in configured synthetic list | 25 |
| UNUSUAL_MERCHANT | category in configured list | 20 |
| ACCOUNT_NEW | age at occurredAt < 7 days | 15 |

Score = min(100, applicable sum); threshold default 70. Test amount 4,999.99/5,000/9,999.99/10,000; score 69/70/>100; no stacked amount rules; account age exactly 7 days does not trigger. Use fixed event/reference time, persist ruleset version and reason codes. These are illustrative, not validated fraud policy.

## End-to-end smoke
Authenticate user/source → create account → submit transaction → list alert → move to review → create/resolve case → query audit. Include unauthorized and conflict scenarios.

## OPEN QUESTIONS
Approve rule features/weights; CI Docker availability; define retention and suitable concurrency test execution strategy.
