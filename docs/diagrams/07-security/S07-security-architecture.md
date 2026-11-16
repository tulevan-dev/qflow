# S07 — Security Architecture

> Nguồn: mục "7. SECURITY ARCHITECTURE" (luồng Login → Access → Refresh → Authz và bảng 10 câu hỏi quyết định).

## A. Mục tiêu
- Mô tả token lifecycle, nơi lưu token, refresh rotation + reuse detection, logout, phân quyền hai lớp (gateway/service) và chống leo thang quyền.
- Là nền cho **Phase 1 — Identity + Gateway** và mọi kiểm tra ownership ở các phase sau.
- Liên hệ: S01 (role), S05 (refresh_tokens), S08 (frontend lưu token), S10 (#10 webhook giả mạo, #19 bot).

## B. Overview diagram

### S07-D1 — Chuỗi bảo mật tổng quát
```mermaid
flowchart LR
  L["Login<br/>verify password Argon2id hoặc BCrypt cost ≥ 12"] --> AT["Access Token<br/>JWT RS256, khoảng 10–15 phút<br/>lưu trong memory"]
  L --> RT["Refresh Token<br/>opaque 256-bit, 7–14 ngày<br/>httpOnly Secure SameSite=Strict cookie<br/>path=/auth/refresh"]
  AT --> GWC["Gateway<br/>chữ ký, exp, rate limit, role thô theo route"]
  GWC --> SVC["Service<br/>verify lại JWT bằng JWKS + ownership"]
  RT -->|"hết access token"| RF["Refresh rotation + reuse detection"]
  RF --> AT
```

## C. Detailed diagrams

### S07-D2 — Login
```mermaid
sequenceDiagram
  autonumber
  actor U as User
  participant W as Next.js
  participant GW as Gateway
  participant ID as identity
  participant DB as identity-db
  U->>W: nhập email và mật khẩu
  W->>GW: login request, endpoint chưa quy định
  GW->>GW: rate limit login
  GW->>ID: chuyển tiếp
  ID->>DB: tìm user theo lower email
  alt sai thông tin hoặc user bị LOCKED
    ID-->>W: 401 hoặc lỗi theo error model
  else đúng
    ID->>ID: verify password hash
    ID->>ID: tạo access JWT RS256 với sub, roles, jti
    ID->>DB: lưu hash refresh token, family_id mới, expires_at
    ID-->>W: access token trong body, refresh token qua cookie httpOnly
  end
  W->>W: giữ access token trong memory
  Note over ID: TTL chính xác, endpoint, ngưỡng lock sau N lần sai: SOURCE CHƯA QUY ĐỊNH
```

### S07-D3 — Request có xác thực đi qua hai lớp kiểm tra
```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant GW as Gateway
  participant S as Service nghiệp vụ
  participant ID as identity JWKS
  C->>GW: request kèm Bearer access token
  GW->>GW: xóa header nội bộ giả mạo như X-User-Id nếu đến từ ngoài
  GW->>GW: verify chữ ký, exp, rate limit, role thô theo route
  alt token sai hoặc hết hạn hoặc vượt rate limit
    GW-->>C: 401 hoặc 429 kèm Retry-After
  else hợp lệ
    GW->>S: forward kèm correlation id
    S->>ID: lấy JWKS nếu chưa cache
    S->>S: verify lại JWT, lấy userId và role từ JWT
    S->>S: kiểm tra ownership hoặc ABAC
    alt không sở hữu resource
      S-->>C: 403
    else được phép
      S-->>C: kết quả
    end
  end
```

### S07-D4 — Refresh rotation và reuse detection
```mermaid
sequenceDiagram
  autonumber
  participant W as Next.js
  participant ID as identity
  participant DB as identity-db
  W->>ID: refresh với cookie refresh token
  ID->>DB: tìm theo hash token
  alt không tồn tại hoặc hết hạn
    ID-->>W: 401, đăng nhập lại
  else token đã bị revoke hoặc đã rotate (dùng lại token cũ)
    ID->>DB: revoke toàn bộ token cùng family_id
    ID-->>W: 401, buộc login lại
  else token đang hợp lệ
    ID->>DB: revoke token cũ, set replaced_by
    ID->>DB: lưu token mới cùng family_id
    ID-->>W: access token mới + refresh token mới qua cookie
  end
```

### S07-D5 — Vòng đời một refresh token
> **ĐỀ XUẤT BỔ SUNG:** tên state là cách mô hình hóa từ các cột `revoked_at`, `replaced_by`, `expires_at` và `family_id` của source; source không định nghĩa state machine này.
```mermaid
stateDiagram-v2
  [*] --> ACTIVE : cấp khi login hoặc khi rotate
  ACTIVE --> ROTATED : refresh thành công, revoke và set replaced_by
  ACTIVE --> EXPIRED : quá expires_at
  ACTIVE --> LOGGED_OUT : logout revoke
  ROTATED --> FAMILY_REVOKED : bị dùng lại, reuse detection
  FAMILY_REVOKED --> [*]
  EXPIRED --> [*]
  LOGGED_OUT --> [*]
```

### S07-D6 — Logout và hiệu lực thu hồi
```mermaid
flowchart TB
  LO["Logout"] --> R1["Revoke refresh token và family"]
  R1 --> R2["Không thể refresh nữa"]
  LO --> AT["Access token vẫn hợp lệ đến khi hết hạn, tối đa khoảng 15 phút"]
  AT --> Q{"Cần chặn gấp?"}
  Q -->|"có"| DL["Denylist jti trong Redis — OPTIONAL"]
  Q -->|"không"| W["Chờ access token hết hạn"]
```

### S07-D7 — Trust boundary
```mermaid
flowchart LR
  subgraph EXT["Ngoài hệ thống — không tin"]
    CL["Browser, bot, attacker"]
    MP["Payment provider webhook"]
  end
  subgraph EDGE["Biên"]
    GW["Gateway<br/>verify JWT, rate limit, xóa header giả mạo<br/>là cổng public duy nhất"]
  end
  subgraph NET["Mạng nội bộ Docker — port service không expose ra ngoài"]
    SV["Các service<br/>verify lại JWT, kiểm ownership"]
    SYS["SYSTEM worker<br/>machine token client credentials"]
  end
  CL -->|"HTTPS"| GW
  GW --> SV
  MP -->|"webhook có HMAC + timestamp + dedupe"| SV
  SYS --> SV
```

### S07-D8 — Quyết định phân quyền
```mermaid
flowchart TB
  REQ["Request đã qua gateway"] --> J{"JWT hợp lệ khi service verify lại?"}
  J -->|"không"| D1["401"]
  J -->|"có"| ID["Lấy userId và role từ JWT<br/>KHÔNG tin userId hay role trong body hoặc header client gửi"]
  ID --> RULE{"Loại hành động?"}
  RULE -->|"ORGANIZER sửa event"| O1{"organizer_id = userId?"}
  RULE -->|"EVENT_STAFF check-in"| O2{"Staff được gán cho event?"}
  RULE -->|"ATTENDEE xem hoặc hủy order"| O3{"Order thuộc userId?"}
  RULE -->|"Đổi role"| O4{"Là ADMIN?"}
  RULE -->|"Thao tác nhạy cảm"| O5["Có thể kiểm lại DB thay vì chỉ tin role trong JWT"]
  O1 -->|"không"| D2["403"]
  O2 -->|"không"| D2
  O3 -->|"không"| D2
  O4 -->|"không"| D2
  O4 -->|"có"| A["Cho phép và ghi audit"]
  O1 -->|"có"| OK["Cho phép"]
  O2 -->|"có"| OK
  O3 -->|"có"| OK
```

### S07-D9 — Mối đe dọa → biện pháp
```mermaid
flowchart LR
  T1["Privilege escalation"] --> C1["Role từ JWT, ownership ở service, đổi role chỉ ADMIN + audit, xóa header nội bộ ở gateway"]
  T2["Lộ DB refresh token"] --> C2["Chỉ lưu hash"]
  T3["Token cũ bị dùng lại"] --> C3["Rotation + reuse detection thu hồi family"]
  T4["XSS lấy token"] --> C4["Access token trong memory, refresh trong httpOnly cookie"]
  T5["CSRF"] --> C5["Cookie chỉ cho /auth/refresh, SameSite=Strict, kiểm Origin hoặc custom header"]
  T6["Brute force login"] --> C6["Rate limit login, lock tạm sau N lần sai, Argon2id hoặc BCrypt cost ≥ 12"]
  T7["Webhook giả mạo"] --> C7["HMAC signature + timestamp tolerance + dedupe event id"]
  T8["Giả vé QR"] --> C8["QR chứa ticketId + chữ ký, server vẫn là nguồn sự thật"]
  T9["Lộ secret"] --> C9["Env hoặc Docker secrets, không commit, có .env.example"]
```

## D. Giải thích sơ đồ
- **D1:** access token ngắn hạn giữ trong memory để giảm hậu quả XSS; refresh token dài hạn nằm trong cookie httpOnly nên JavaScript không đọc được.
- **D2:** server chỉ phát refresh token dạng opaque ngẫu nhiên và chỉ lưu **hash** của nó cùng `family_id`.
- **D3:** hai lớp: gateway làm kiểm tra **thô** (có token hợp lệ không, route cần role gì, rate limit); service luôn kiểm **tinh** (có sở hữu resource không). Gateway bị bypass thì service vẫn tự bảo vệ.
- **D4–D5:** mỗi lần refresh sinh token mới, token cũ bị revoke; nếu token cũ xuất hiện lại thì coi là bị đánh cắp và thu hồi cả family.
- **D6:** logout bằng JWT stateless chỉ thật sự chặn được **refresh**; access token sống đến hết hạn trừ khi dùng denylist (OPTIONAL).
- **D7–D9:** header như `X-User-Id` chỉ đáng tin khi do gateway tự gắn sau khi xóa bản từ bên ngoài.

## E. Những điều phải nhớ
- Không lưu JWT trong localStorage; không lưu refresh token plaintext; không tin role trong body; không để JWT sống lâu.
- Phân quyền chỉ ở UI là **không** phải bảo mật (S08).
- IDOR là lỗi điển hình: staff check-in sự kiện không được gán, user xem order người khác (bug #10, S18).
- **SOURCE CHƯA QUY ĐỊNH:** endpoint login/register/logout/refresh chi tiết (chỉ có path cookie `/auth/refresh`), TTL chính xác, ngưỡng rate limit và lock tài khoản, đường dẫn JWKS, cách xoay khóa RS256, danh sách header nội bộ đầy đủ, cách service lấy và cache JWKS, claim ngoài `sub`/`roles`/`jti`.
- Mâu thuẫn nhỏ cần để ý: bảng nói "Argon2id (hoặc BCrypt cost ≥ 12)" nên thuật toán chưa chốt.

## F. Liên kết
- Đầu vào: S01 (role), S05 (refresh_tokens). Đầu ra: **Phase 1**, S08 (token phía client), S10, S18 (bug auth).

## G. Checklist
- [x] token lưu đâu  - [x] refresh xử lý  - [x] logout  - [x] expiration  - [x] password hashing  - [x] gateway check gì  - [x] service check gì  - [x] chống privilege escalation  - [x] service-to-service, secret, webhook, QR
