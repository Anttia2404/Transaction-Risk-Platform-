# PRD — Transaction Risk Platform

## Problem
Mô phỏng hệ thống nhận giao dịch giả lập, phát hiện giao dịch đáng ngờ bằng rules minh bạch, và hỗ trợ Risk Analyst điều tra. Không chuyển tiền thật, không dùng dữ liệu khách hàng thật.

## Người dùng
- **Analyst:** xem/xử lý alert, tạo case, ghi kết luận.
- **Admin:** quản lý users/accounts và cấu hình threshold.
- **Simulator:** gửi giao dịch synthetic đã xác thực theo source.

## MVP
JWT auth; roles ANALYST/ADMIN; accounts giả lập; transaction intake; PostgreSQL-backed idempotency; deterministic rule engine (threshold mặc định 70); alert workflow; one-to-one case; audit log; PostgreSQL/Flyway; Docker Compose. Backend/API trước, Next.js sau.

Ngoài phạm vi: ML/FastAPI scoring, real payment/PII, Kafka, Kubernetes, microservices, Redis bắt buộc, SSO/MFA, streaming, production SLA.

## User stories
1. Simulator gửi giao dịch với source và external ID để hệ thống nhận diện duy nhất.
2. Simulator retry cùng idempotency key sau timeout và không tạo dữ liệu trùng.
3. Hệ thống tính score xác định, lưu reason/version và tạo alert khi đạt ngưỡng.
4. Analyst đăng nhập, lọc alert, cập nhật trạng thái an toàn khi có concurrent edits.
5. Analyst tạo tối đa một case cho alert và lưu kết luận.
6. Người có quyền truy audit để biết actor, hành động và thời điểm.

## Business rules
1. `(source, external_transaction_id)` unique; source lấy từ credential đã xác thực.
2. `Idempotency-Key` bắt buộc; unique trong `(source,key)` scope. Cùng key/cùng canonical payload replay; cùng key/khác payload trả 409.
3. Amount > 0, PostgreSQL `NUMERIC(19,4)`/Java `BigDecimal`; currency uppercase 3 ký tự.
4. Account phải tồn tại và ACTIVE.
5. Score integer 0–100; `score >= threshold` tạo đúng một alert.
6. Rule scoring deterministic, versioned; transaction lưu score/reasons snapshot.
7. Alert transitions: NEW → IN_REVIEW → RESOLVED hoặc FALSE_POSITIVE. **OPEN QUESTION:** có cho phép NEW đi thẳng terminal không? Đề xuất không.
8. Một alert có tối đa một case; case trỏ đúng một alert. DB unique FK bảo vệ invariant.
9. Transaction, scoring snapshot, optional alert, idempotency response và audit ghi atomically trong PostgreSQL.
10. Mọi state change, kết luận case, threshold change ghi audit; không ghi secrets/PII.

## Functional requirements
FR1 login/current-user bằng JWT. FR2 quản lý account status. FR3 validate/intake/idempotency. FR4 score và reason codes. FR5 list/detail/filter/page alert và state update. FR6 case create/update. FR7 audit query append-only.

## Non-functional requirements
Security: password hashing, default-deny authorization, secrets ngoài Git. Consistency: database constraints + transactions. Maintainability: module boundary rõ. Testability: JUnit/Mockito/Testcontainers PostgreSQL. Observability: correlation ID, log an toàn. Synthetic data only; list APIs phân trang; chưa đặt production SLO.

## Acceptance criteria
- Concurrent same-key submission tạo tối đa một transaction/alert/audit set.
- Same key/payload replay; different payload conflict.
- Score dưới ngưỡng không tạo alert; bằng hoặc trên tạo một alert.
- Lỗi insert alert/audit rollback toàn bộ nghiệp vụ đồng bộ.
- JWT invalid/expired/missing → 401; role thiếu → 403.
- Stale alert version → 409, không lost update.
- Concurrent case create cho cùng alert chỉ một thành công.
- Chưa tuyên bố test đã chạy trước implementation.

## OPEN QUESTIONS
- Source API key provisioning: đề xuất key riêng theo simulator/source.
- Admin có được làm analyst workflow không? Đề xuất không, trừ khi có cả hai role.
- Có cần assign alert trong MVP? Đề xuất hoãn.
- Rule inputs/weights cần xác nhận; xem `TEST_STRATEGY.md`.
- Audit/idempotency retention cần chốt trước khi dùng ngoài demo.
