# Transaction Risk Platform

Java 21 Spring Boot Modular Monolith foundation. The current milestone only starts the application, connects to local PostgreSQL, applies a minimal Flyway migration, and exposes an Actuator health endpoint. Business modules are package skeletons only.

## Requirements

- JDK 21 (project build target)
- Docker Desktop with Docker Compose
- No system Maven installation is required; use Maven Wrapper

Verify Java with `java -version`. The output should report version 21. Set `JAVA_HOME` to the JDK 21 directory and put its `bin` directory first on `PATH` for the shell running Maven. The existing Java 23 installation is not modified by this project.

## Start PostgreSQL

In PowerShell:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env } else { Write-Output '.env already exists; leaving it unchanged.' }
docker compose up -d postgres
docker compose ps
```

`.env` is local-only and ignored by Git. The example password is a disposable development value; do not use it in a shared or deployed environment. PostgreSQL is bound to `127.0.0.1:5433` on the host (the container listens on 5432) and data persists in the `postgres_data` volume.

## Run the application

```powershell
.\mvnw.cmd spring-boot:run
```

Default local DB settings match `.env.example`. To override them for the application process:

```powershell
$env:DB_URL = 'jdbc:postgresql://localhost:5433/transaction_risk'
$env:DB_USERNAME = 'transaction_risk'
$env:DB_PASSWORD = 'local_dev_only_change_me'
.\mvnw.cmd spring-boot:run
```

Check health at <http://localhost:8080/actuator/health>. Only the health Actuator endpoint is exposed over HTTP.

## Build and tests

```powershell
.\mvnw.cmd clean verify
```

The integration test starts a disposable PostgreSQL 17 container through Testcontainers, starts the Spring application on a random port, confirms Flyway applied migration V1, and checks the HTTP health endpoint. Docker must be running. The first Wrapper/build/test run downloads Maven and project dependencies.

On Unix-like shells, use `./mvnw` instead.

## Migration scope

`V1__foundation_baseline.sql` executes a harmless PostgreSQL `SELECT 1` to verify Flyway migration execution. It deliberately creates no domain tables. Business schema migrations belong to later approved milestones.

## Current limitations

- No authentication, transaction API, risk rules, alerts, cases, or frontend are implemented.
- The app's local DB fallback is for disposable development only; use environment-provided credentials outside local development.
- Java 21 is the required runtime/build baseline. A successful build on another JDK does not verify Java 21 compatibility.
