# Security Design

## MVP controls
- Stateless Spring Security `SecurityFilterChain`, default deny; public login and health only. Analyst/admin use JWT; simulator uses separate source credential.
- Short-lived signed access JWT (recommend 15 min) with subject, roles, iat/exp and issuer. Validate signature, expiry, nbf, issuer; no secrets or mutable business data in claims. Invalid/missing/expired → 401; valid but insufficient role → 403.
- Login password hashed with BCrypt or Argon2id; generic login failure; never log credentials.
- Role matrix: ANALYST reads/processes alerts and cases; ADMIN manages users/accounts/threshold and audits. Least privilege: ADMIN does not automatically gain Analyst actions unless assigned both roles.
- Source key maps to authenticated source; never trust payload source. Store hash if persisted; redact and rotate/revoke keys.
- CORS allowlist for future UI. CSRF may be disabled for stateless bearer API when browser does not send credentials automatically. Parameterized queries and request validation.
- Audit semantic actor/action/entity/time/correlation and minimal before/after. No tokens, passwords, API keys, real PII or full request body.
- Local secrets in ignored `.env`; AWS later Secrets Manager/Parameter Store + IAM roles. No secret in Git/image/CI logs.

## Token policy
MVP omits refresh token: re-login after expiry. Future refresh should be opaque, rotated, hashed at rest, revocable with reuse detection and protected client storage. HTTPS required on deployed environment.

## MVP vs future hardening
MVP: above controls, synthetic data, source credential, rate-limit consideration. Future production: managed key rotation, MFA/SSO, WAF/rate limits, penetration testing, formal retention, tamper-evident audit archive, security monitoring and fine-grained scopes. This design does not claim production readiness.

## OPEN QUESTIONS
JWT signing algorithm/key lifecycle; source credential registry vs env mapping; admin/analyst role combination; login throttling/lockout policy.
