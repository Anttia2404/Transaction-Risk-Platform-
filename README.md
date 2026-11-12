# Transaction Risk Platform

Java 21 / Spring Boot 3.5 modular monolith. M1 foundation and M2 authentication/security are implemented. The application connects to PostgreSQL, migrates schema with Flyway, exposes a health endpoint, and provides JWT login/current-user APIs. Business modules beyond authentication remain package skeletons.

## Requirements

- JDK 21 (project build target)
- Docker Desktop with Docker Compose
- No system Maven installation is required; use Maven Wrapper
- A Base64-encoded random JWT signing key containing at least 32 bytes

Verify Java with `java -version`. The output should report version 21. Set `JAVA_HOME` to the JDK 21 directory and put its `bin` directory first on `PATH` for the shell running Maven. The existing Java 23 installation is not modified by this project.

## Start PostgreSQL

In PowerShell:

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env } else { Write-Output '.env already exists; leaving it unchanged.' }
docker compose up -d postgres
docker compose ps
```

`.env` is local-only and ignored by Git. The example database password is a disposable development value; do not use it in a shared or deployed environment. PostgreSQL is bound to `127.0.0.1:5433` on the host (the container listens on 5432) and data persists in the `postgres_data` volume. Spring loads this file for local configuration.

## Run the application

Create a random signing key and set it for the application process. The key stays in your shell or ignored `.env`; never commit it:

```powershell
$jwtKeyBytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($jwtKeyBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
```

Then run the application:

```powershell
.\mvnw.cmd spring-boot:run
```

For the explicitly opt-in local demo user, set local credentials in `.env` and enable the local profile:

```powershell
# Add values to the ignored .env file:
# LOCAL_USER_SEED_ENABLED=true
# LOCAL_USER_SEED_USERNAME=admin
# LOCAL_USER_SEED_PASSWORD=<a local password>
# LOCAL_USER_SEED_ROLE=ADMIN
$env:SPRING_PROFILES_ACTIVE = 'local'
.\mvnw.cmd spring-boot:run
```

Without the `local` profile and `LOCAL_USER_SEED_ENABLED=true`, no user is seeded. The seed inserts only when the normalized username does not already exist.

Check health at <http://localhost:8080/actuator/health>. Only the health Actuator endpoint is exposed over HTTP.

## Build and tests

```powershell
.\mvnw.cmd clean verify
```

The integration tests start a disposable PostgreSQL 17 container through Testcontainers, confirm Flyway applied V1 and V2, and exercise login, current-user, password hashing, JWT validation, role authorization, default-deny behavior, public health and stateless requests. Docker must be running. The first Wrapper/build/test run downloads Maven and project dependencies.

On Unix-like shells, use `./mvnw` instead.

## Authentication APIs

- `POST /api/v1/auth/login` accepts `username` and `password`, returning a short-lived 900-second HS256 bearer token and user summary.
- `GET /api/v1/auth/me` requires a valid bearer token and reloads the enabled user from PostgreSQL.
- Missing/invalid tokens return 401; authenticated requests without an allowed policy return 403. Only login and health are public. M2 defines no business endpoints.
- Tokens are stateless. There is no refresh or immediate revocation list. Disabling a user blocks password login and `/me`, but an issued token can still be accepted by any future endpoint until its expiry unless that endpoint also checks user state. Keep the 900-second expiry short and plan revocation support before protected business workflows need immediate disablement.

## Migration scope

`V1__foundation_baseline.sql` verifies Flyway migration execution without creating tables. `V2__create_users.sql` adds only the authentication users table. Business schema migrations belong to later approved milestones.

## Current limitations

- No transaction API, risk rules, alerts, cases, or frontend are implemented.
- JWT key provisioning/rotation and immediate token revocation are not implemented; keep the key private and rotate it with awareness that doing so invalidates all active tokens.
- Java 21 is the required runtime/build baseline. A successful build on another JDK does not verify Java 21 compatibility.
