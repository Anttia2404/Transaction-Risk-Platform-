# ADR-002: PostgreSQL-backed Idempotency

- **Status:** Accepted for MVP
- **Context:** retries after timeout and simultaneous requests must not duplicate business effects; PostgreSQL is authoritative store.
- **Decision:** unique `(source,idempotency_key)`, canonical request hash and completed response stored atomically with transaction/alert/audit; also unique `(source,external_transaction_id)`.
- **Alternatives:** memory (lost on restart/multi-instance unsafe); Redis (extra dependency, not atomic with DB); client-only dedupe (not trustworthy).
- **Consequences/trade-offs:** crash-after-commit replay and database consistency; must implement conflict/locking carefully and define retention/canonicalization. Does not make external effects atomic; MVP has none.
