# ADR-001: Modular Monolith

- **Status:** Accepted for MVP
- **Context:** one developer/team; transaction, alert, audit require atomicity; portfolio should remain easy to run.
- **Decision:** one Spring Boot deployable with auth/accounts/transactions/risk/alerts/cases/audit boundaries and PostgreSQL.
- **Alternatives:** microservices (independent deployment but distributed consistency/ops burden); unstructured monolith (easy start, weak boundaries).
- **Consequences/trade-offs:** atomic DB transaction and simple local deploy; no independent scale/deploy. Maintain explicit interfaces and revisit only with demonstrated need.
