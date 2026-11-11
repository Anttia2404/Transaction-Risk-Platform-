# Project guidance

- **Stack:** Java 21, Spring Boot, PostgreSQL, Flyway, JUnit 5, Mockito, Testcontainers, Docker Compose. Next.js/TypeScript and Python FastAPI are later phases. Redis optional only for a justified use case.
- **Modules:** modular monolith: auth, accounts, transactions, risk, alerts, cases, audit. Keep APIs explicit; never access another module's repository directly.
- **Commands:** Windows build/test: `.\mvnw.cmd clean verify`; run locally: `.\mvnw.cmd spring-boot:run`; start PostgreSQL: `docker compose up -d postgres`. On Unix-like systems use `./mvnw` for Maven commands. Docker must be running for Testcontainers tests.
- **Limits:** planning approval precedes source implementation. No Kafka, Kubernetes, microservices, or unapproved dependencies. Synthetic data only. Keep transaction, risk snapshot, alert and audit atomic where applicable.
- **Architecture:** do not alter stack/module/security/API decisions or add dependencies without documenting rationale and getting project owner approval.
- Keep detailed design in `docs/`; this file is concise contributor guidance only.
