# REST API Contract

Base `/api/v1`; JSON camelCase; UTC ISO-8601. Protected user APIs use `Authorization: Bearer <JWT>`. Simulator uses separate source API key. Use `application/problem+json` errors:

```json
{"type":"about:blank","title":"Conflict","status":409,"code":"IDEMPOTENCY_KEY_REUSED","detail":"Key payload differs","instance":"/api/v1/transactions","correlationId":"req-1","fieldErrors":[]}
```

## Auth
- `POST /auth/login` public; body `{"username":"analyst","password":"..."}`; 200 `{accessToken,tokenType:"Bearer",expiresIn,user:{id,username,roles}}`; 400 invalid shape, 401 generic credential failure.
- `GET /auth/me` authenticated; 200 `{id,username,roles}`; invalid/expired/missing token 401.
- Refresh omitted MVP; short-lived access token then login again. Future refresh: rotating opaque token hashed at rest/revocable.

## Accounts
- `POST /accounts` ADMIN; `{accountRef}` → 201 `{id,accountRef,status:"ACTIVE",createdAt}`; 400 validation, 409 duplicate.
- `GET /accounts/{id}` ANALYST/ADMIN → 200 or 404.
- `PATCH /accounts/{id}` ADMIN; `{status:"SUSPENDED"}` → 200; validate enum.

## Transactions
- `POST /transactions` source credential + mandatory `Idempotency-Key` (1–128 visible ASCII chars).
Request: `{"externalTransactionId":"sim-001","accountRef":"sim-acct-1","amount":"1250.0000","currency":"USD","occurredAt":"2026-11-09T08:30:00Z","merchantCategory":"ELECTRONICS","countryCode":"US"}`.
Source comes from credential, not body. Validate amount positive/max 4 decimals, currency 3 uppercase letters, timestamp, lengths, active account.
201 example: `{"transactionId":"uuid","source":"sim-a","externalTransactionId":"sim-001","risk":{"score":82,"threshold":70,"decision":"ALERT","ruleSetVersion":"v1","reasons":["HIGH_AMOUNT"]},"alertId":"uuid","createdAt":"..."}`.
- Same source/key + same canonical payload: replay original status/body (recommend original 201).
- Same key + different payload: 409 `IDEMPOTENCY_KEY_REUSED`.
- Different key + same `(source,externalTransactionId)`: 409 `DUPLICATE_EXTERNAL_TRANSACTION`.
- Concurrent same-key: unique constraint permits one execution; other waits/resolves to committed replay. If timeout, retry same key.
- Rollback removes claim; commit before network timeout remains replayable. Never retry with new key after ambiguous result.
- Other errors: 400 validation, 401 source auth, 404 account, 409 suspended/duplicate, 500 unexpected.
- `GET /transactions/{id}` ANALYST/ADMIN → detail including immutable risk snapshot.
- `GET /transactions?accountId=&from=&to=&page=0&size=20` ANALYST/ADMIN → page envelope.

## Alerts
- `GET /alerts?status=&from=&to=&minScore=&page=0&size=20` ANALYST/ADMIN; stable sort createdAt,id.
- `GET /alerts/{id}` ANALYST/ADMIN → detail or 404.
- `PATCH /alerts/{id}` ANALYST; request `{"status":"IN_REVIEW","version":0,"comment":"Review started"}`; 200 updated state/version/time; 400 invalid, 409 stale version or invalid transition.

## Cases
- `POST /cases` ANALYST; `{"alertId":"uuid","summary":"Unusual pattern"}`; 201 case OPEN/version 0; 409 `CASE_ALREADY_EXISTS` for concurrent/duplicate create.
- `GET /cases/{id}` and `GET /cases?status=&page=&size=` ANALYST/ADMIN.
- `PATCH /cases/{id}` ANALYST; `{"status":"RESOLVED","resolution":"CUSTOMER_CONFIRMED","summary":"...","version":0}`; resolution required when resolved; stale/invalid transition 409.

## Audit
- `GET /audit-logs?entityType=&entityId=&actorId=&from=&to=&page=&size=` ADMIN (or explicit audit permission). Read-only, safe before/after data. 401/403 as applicable.

## Conventions / open questions
Page starts 0, default 20, max 100; allowlist sort/filter fields. Prefer 400 for all input validation; reserve 409 for conflict. Confirm source key provisioning, transaction synthetic signal fields, direct NEW→terminal transitions, and version in body vs ETag.
