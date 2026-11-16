# PHASE 1 — Lỗi thường gặp, checklist, phạm vi loại trừ, tự kiểm tra (Mục 17–20)

## Mục 17. Các lỗi triển khai thường gặp
**Từ source (Common mistakes Phase 1):** lưu JWT trong localStorage · refresh token lưu plaintext · tin role từ request body · JWT sống quá lâu.

**ĐỀ XUẤT BỔ SUNG — các lỗi thực tế khác:**
| Nhóm | Lỗi | Hậu quả | Cách tránh |
|---|---|---|---|
| Token | Không ép `alg`, chấp nhận `alg=none`/HS256 | Giả được token | Cấu hình chỉ RS256, chọn khóa theo `kid` |
| Token | Không kiểm `iss`/`aud`/`exp` | Dùng token của hệ khác | Bật và test SEC-05 |
| Token | Không có `kid` | Không xoay được khóa | Luôn gán `kid` |
| Token | Gọi JWKS mỗi request hoặc không giới hạn refetch khi `kid` lạ | Chậm hoặc tự DoS | Cache TTL + giới hạn refetch |
| Refresh | SELECT rồi UPDATE ở tầng ứng dụng | Hai request cùng nhận token mới | UPDATE có điều kiện, kiểm số dòng |
| Refresh | Xóa dòng token thay vì revoke | Mất khả năng phát hiện reuse | Chỉ set `revoked_at` |
| Refresh | Quên kiểm user còn ACTIVE khi refresh | Tài khoản bị khóa vẫn gia hạn phiên | Kiểm trong cùng luồng refresh |
| Refresh | Dùng `Random` thay nguồn ngẫu nhiên an toàn | Đoán được token | Dùng nguồn an toàn 256-bit |
| Cookie | `Path` không khớp đường dẫn refresh (ví dụ `/auth/refresh` nhưng API là `/api/v1/auth/refresh`) | Trình duyệt không gửi cookie | Chốt đường dẫn, đặt Path khớp, có test |
| Cookie | Xóa cookie sai thuộc tính | Cookie còn lại sau logout | Dùng đúng Path/thuộc tính khi xóa |
| Cookie | Web và API khác site + `SameSite=Strict` | Cookie không bao giờ gửi | Cùng site, hoặc thiết kế lại có chủ đích |
| CORS | `Allow-Origin: *` cùng `Allow-Credentials` | Mở cửa cho mọi origin | Allowlist cụ thể |
| Mật khẩu | BCrypt cắt ở 72 byte; không giới hạn độ dài | Cắt cụt hoặc DoS CPU | Giới hạn độ dài tối đa |
| Mật khẩu | Cost quá thấp | Dễ brute force offline | Cost ≥ 12 hoặc Argon2id, đo trên máy bạn |
| Auth | Thông báo khác nhau cho "không có user"/"sai mật khẩu" | Dò được email | Thông báo chung + hash giả |
| RBAC | Thiếu tiền tố `ROLE_` khi ánh xạ | `hasRole` luôn sai/đúng bất ngờ | Test UT-07 |
| RBAC | Chỉ kiểm ở gateway | Gọi thẳng service là qua | Method security + verify lại JWT |
| RBAC | Nhận `roles` từ body | Tự nâng quyền | Bỏ qua field, lấy từ DB/JWT |
| Gateway | Sanitize sau khi đã đọc header | Header giả lọt | Đặt sanitize trước tiên |
| Gateway | Tin `X-Forwarded-For` từ client | Né rate limit | Chỉ tin proxy cấu hình |
| Dữ liệu | UNIQUE email chỉ ở code | Race tạo trùng | UNIQUE `lower(email)` ở DB |
| Dữ liệu | Entity trả thẳng qua API | Lộ hash | DTO riêng |
| Vận hành | Log token/mật khẩu | Lộ bí mật | Bộ lọc log, test SEC-20 |
| Vận hành | Commit khóa private/`.env` | Lộ khóa | `.gitignore`, quét secret CI |
| Kiểm thử | Chỉ test happy path; tắt test flaky | Bug bảo mật lọt | Test SEC/NEG/ABU, sửa chứ không tắt |

## Mục 18. Checklist
### 18.1 Chạy local
- [ ] `docker compose up -d` chạy Postgres, Redis (và các thành phần Phase 0)
- [ ] Tạo cặp khóa RSA cho dev bằng script, lưu **ngoài** git, trỏ đường dẫn bằng biến môi trường
- [ ] Khởi động identity: migration Flyway chạy, `health` UP
- [ ] Khởi động gateway: route tới identity và hello hoạt động, JWKS lấy được
- [ ] Tạo ADMIN đầu tiên theo quyết định 10 (không để mật khẩu trong repo)
- [ ] Chạy tuần tự bằng công cụ HTTP bất kỳ (đường dẫn là đề xuất):
  1. register → 201
  2. login (lưu cookie jar) → 200 + `Set-Cookie`
  3. gọi `GET /users/me` bằng access token → 200
  4. refresh → 200, token mới; **lặp lại refresh bằng cookie cũ** → 401 và phiên chết
  5. login lại, logout, refresh → 401
  6. ATTENDEE gọi endpoint admin → 403; ADMIN → 200
  7. nộp hồ sơ organizer → ADMIN approve → refresh → token có `ORGANIZER`
  8. gọi login quá ngưỡng → 429 có `Retry-After`
  9. gửi `X-User-Id` giả qua gateway → không có tác dụng

### 18.2 Kiểm thử
- [ ] Toàn bộ UT, IT, SEC, NEG xanh bằng một lệnh; chạy test race lặp ≥ 20 lần
- [ ] Không test nào bị tắt/bỏ qua
- [ ] Quét secret xanh
- [ ] OpenAPI lint xanh, contract test xanh

### 18.3 Review (tự review theo S20)
- [ ] Ownership/role: mọi endpoint có quy tắc rõ ràng (bảng 12.1 khớp code)?
- [ ] Có gọi mạng trong transaction nào không?
- [ ] Mọi thao tác nhạy cảm (đổi role, duyệt organizer) có log không chứa dữ liệu nhạy cảm?
- [ ] Timeout/rate limit đã có giá trị rõ và nằm trong cấu hình?
- [ ] Lỗi có mã/ý nghĩa rõ, không lộ nội bộ?
- [ ] Có abstraction thừa (interface 1 implementation "phòng khi")?
- [ ] ADR cập nhật; migration backward compatible
- [ ] Không có secret trong diff

## Mục 19. Chưa thuộc Phase 1 — không tự triển khai sớm
| Nội dung | Vì sao chưa | Thuộc |
|---|---|---|
| Kiểm ownership theo sự kiện (ORGANIZER sửa event của mình, STAFF theo event được gán) | Chưa có catalog | Phase 2 (và ticket ở Phase 7) |
| Kiểm điều kiện "organizer đã VERIFIED" khi submit sự kiện | Thuộc workflow catalog | Phase 2 |
| Giao diện đăng nhập, silent refresh single-flight, route guard | Frontend | Phase 3 |
| Idempotency-Key | Chưa có order | Phase 5 |
| Machine token / client credentials cho SYSTEM | Chưa có worker nội bộ | Phase 5–6 (đề xuất), source chưa chốt |
| Phát event ra Kafka, outbox cho identity | S05 không liệt kê outbox cho identity | Phase 6 (nếu cần) |
| Audit-service và audit log append-only | Chưa có service | Phase 7 (Phase 1 chỉ ghi structured log) |
| Rate limit nâng cao theo user/event, waiting room | Thuộc waiting room | Phase 8 |
| Tracing end-to-end, dashboard, alert | Thuộc observability đầy đủ | Phase 9 (Phase 1 chỉ health/metrics cơ bản) |
| Denylist `jti` trong Redis | OPTIONAL | sau |
| Xoay khóa RS256 tự động | OPTIONAL | sau |
| Xác minh email, quên mật khẩu, MFA | Ngoài phạm vi | không thuộc roadmap |
| OAuth2/OIDC social login, Keycloak | OPTIONAL / DO NOT BUILD YET | S22 |
| Bot mitigation, captcha | Advanced | sau |
| Triển khai production, HTTPS/Caddy | Phase 11 | Phase 11 |

## Mục 20. Câu hỏi tự kiểm tra
Tự trả lời **bằng lời của bạn** trước, sau đó đối chiếu "ý cần có". Câu 1–5 là câu phỏng vấn mẫu của source.

| # | Câu hỏi | Ý cần có trong câu trả lời |
|---|---|---|
| 1 | Access token khác refresh token thế nào? | TTL, nơi lưu, stateless vs có trạng thái ở DB, mục đích |
| 2 | Vì sao dùng rotation? | Giảm cửa sổ bị lợi dụng, phát hiện token bị đánh cắp |
| 3 | Logout với JWT stateless thực sự làm gì? | Chỉ thu hồi refresh, access sống tới hết hạn; denylist là tùy chọn |
| 4 | Privilege escalation là gì và hệ thống này chặn thế nào? | Nâng quyền trái phép; role từ JWT/DB, method security, đổi role chỉ ADMIN, không tin body/header ngoài |
| 5 | BCrypt vs Argon2id? | Memory-hard, cấu hình cost, giới hạn 72 byte của BCrypt |
| 6 | Vì sao RS256 + JWKS thay vì HS256? | Chỉ identity giữ khóa private, service chỉ cần public |
| 7 | Token family là gì và reuse detection hoạt động ra sao? | `family_id`, `replaced_by`, token revoked bị dùng lại ⇒ thu hồi cả family |
| 8 | Vì sao phải revoke chứ không xóa dòng token? | Giữ bằng chứng để nhận diện reuse |
| 9 | Hai tab cùng refresh một lúc thì chuyện gì xảy ra, giải quyết thế nào? | Race, UPDATE có điều kiện, single-flight phía client, grace window |
| 10 | Vì sao gateway **và** service đều verify JWT? | Gateway bị bypass, trust boundary, kiểm thô/tinh |
| 11 | Vì sao phải xóa `X-User-Id` từ client? | Header spoofing, chỉ header do gateway gắn đáng tin |
| 12 | Vì sao refresh cookie là `httpOnly`, `Secure`, `SameSite=Strict` và có `Path`? | XSS, MITM, CSRF, thu hẹp phạm vi gửi |
| 13 | Khi nào `SameSite=Strict` làm hỏng luồng, và bạn xử lý ra sao? | Web/API khác site; thiết kế cùng site hoặc cách khác |
| 14 | CSRF có áp dụng cho Bearer access token không? Cho refresh cookie thì sao? | Bearer không tự gửi bởi trình duyệt; cookie tự gửi nên cần phòng |
| 15 | Vì sao thông báo lỗi login phải chung? | Chống user enumeration; cùng với hash giả để giảm timing |
| 16 | Khóa tài khoản và rate limit IP khác nhau ở đâu? | Một nguồn nhiều tài khoản vs nhiều nguồn một tài khoản; rủi ro DoS tài khoản |
| 17 | Role mới của user có hiệu lực khi nào? Trade-off? | Sau refresh/khi hết TTL access; stateless đổi lấy độ trễ |
| 18 | Điều gì xảy ra nếu khóa private bị lộ? | Giả token; cần xoay khóa theo `kid`, thu hồi phiên; vì sao cần hỗ trợ ≥ 2 khóa |
| 19 | Vì sao cache JWKS có TTL và vì sao cần giới hạn refetch? | Hiệu năng, chịu lỗi identity tạm thời, chống flood `kid` |
| 20 | Phase 1 chưa làm gì mà bạn sẽ phải làm ở Phase 2–8 liên quan đến auth? | Ownership, machine token, audit, rate limit nâng cao... (mục 19) |

## E. Những điều phải nhớ
- Không học thuộc: mỗi câu phải trả lời được "vì sao" và nêu **trade-off**.
- **SOURCE CHƯA QUY ĐỊNH:** đáp án chuẩn và điểm đạt của interview checkpoint; mình sẽ tạo bộ đầy đủ (10 lý thuyết, 10 project, 5 debugging, 5 system design, 5 production) khi bạn báo xong Phase 1.
