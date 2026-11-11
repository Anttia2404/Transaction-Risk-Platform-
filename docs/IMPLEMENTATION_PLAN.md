# Backend MVP Implementation Plan

Planning only; implementation and test execution await approval.

## M1 — Foundation
- **Goal:** Java 21/Spring Boot runnable shell, PostgreSQL and Flyway.
- **Learn:** Spring lifecycle/DI, profiles, migrations, Compose.
- **Modules:** skeleton auth/accounts/transactions/risk/alerts/cases/audit.
- **Work:** build wrapper, health endpoint, local DB, first migration/config example.
- **Acceptance:** clean local startup, migration applies, health responds, no secret committed.
- **Tests:** context startup, Flyway PostgreSQL Testcontainers smoke.
- **DoD:** reproducible local instructions and passing foundation tests.

## M2 — Auth/security
- **Goal:** JWT login/current user, roles and default deny.
- **Learn:** Spring Security, hashing, JWT, 401/403.
- **Modules:** auth.
- **Work:** user persistence, password hashing, token issue/validation, role checks, local demo seed.
- **Acceptance:** valid login; invalid/expired token 401; role mismatch 403.
- **Tests:** auth/security cases in TEST_STRATEGY.
- **DoD:** documented role matrix; secrets not logged/committed.

## M3 — Accounts/intake
- **Goal:** synthetic accounts and validated source-authenticated transaction.
- **Learn:** REST validation, BigDecimal/Numeric, uniqueness, DB transactions.
- **Modules:** accounts, transactions.
- **Work:** account status, source mapping, transaction persistence and external identity constraints.
- **Acceptance:** valid record once; bad fields/inactive account rejected; duplicate source/external id conflicts.
- **Tests:** unit validation and PostgreSQL constraints.
- **DoD:** error codes documented, synthetic data only.

## M4 — Idempotency/risk
- **Goal:** retry-safe intake and explainable scoring.
- **Learn:** unique insert races, canonical hashes, deterministic functions.
- **Modules:** transactions, risk.
- **Work:** idempotency record/response, concurrent conflict resolution, versioned rules and threshold.
- **Acceptance:** replay, changed payload conflict, one result under concurrency, stable score/reasons.
- **Tests:** idempotency races, external ID race, rule boundaries, rollback.
- **DoD:** ambiguous timeout behavior documented; no network call inside DB transaction.

## M5 — Alerts/audit
- **Goal:** alert lifecycle and atomic audit.
- **Learn:** optimistic locking, state machines, append-only audit.
- **Modules:** alerts, audit, transactions, risk.
- **Work:** list/detail/filter/page, state version check, audit event writes.
- **Acceptance:** threshold correct; stale update 409; failure rolls back transaction/alert/audit.
- **Tests:** Testcontainers, concurrent update and injected rollback.
- **DoD:** sensitive payload excluded from audit.

## M6 — Cases
- **Goal:** one-to-one case workflow.
- **Learn:** unique FK, workflow validation, versioning.
- **Modules:** cases, alerts, audit.
- **Work:** create/list/detail/update/resolution.
- **Acceptance:** one case per alert under race; invalid transition/stale version conflict; audit recorded.
- **Tests:** uniqueness race, transition, audit atomicity.
- **DoD:** DB constraint independently enforces invariant.

## M7 — Demo/CI hardening
- **Goal:** coherent API flow runnable via simulator/curl.
- **Learn:** Problem Details, correlation IDs, pagination, CI.
- **Modules:** all.
- **Work:** error consistency, safe logs, smoke flow, Compose polish, GitHub Actions build/test.
- **Acceptance:** end-to-end synthetic flow works; tests use PostgreSQL; CI passes.
- **Tests:** regression/security/concurrency/E2E smoke.
- **DoD:** setup/design docs and limitations; do not claim production-ready.

Later: Next.js UI, AWS deployment, FastAPI ML adapter. Redis only for demonstrated need; no Kafka/Kubernetes/microservices by default.

**Recommended first:** M1 after review/approval; it establishes reproducibility before business implementation.
