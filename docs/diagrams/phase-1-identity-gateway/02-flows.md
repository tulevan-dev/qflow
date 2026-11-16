# PHASE 1 — Luồng xử lý (Mục 3–10)

> Mỗi mục: sơ đồ → giải thích → nhánh lỗi → bất biến. Endpoint trong sơ đồ là **ĐỀ XUẤT BỔ SUNG** (source chưa định nghĩa endpoint; xem file 03).

## Mục 3. Register / Login

### PH1-D5 — Register
```mermaid
sequenceDiagram
  autonumber
  actor C as Client
  participant GW as Gateway
  participant ID as identity
  participant DB as identity-db
  C->>GW: POST register với email và password
  GW->>GW: sanitize header, rate limit
  GW->>ID: chuyển tiếp, route công khai
  ID->>ID: validate email và password policy, chuẩn hóa lower email
  alt dữ liệu không hợp lệ
    ID-->>C: 400 problem+json kèm lỗi từng field
  else hợp lệ
    ID->>ID: hash mật khẩu
    ID->>DB: 1 transaction: INSERT users + INSERT user_roles ATTENDEE
    alt vi phạm UNIQUE lower email
      DB-->>ID: unique violation
      ID-->>C: 409 problem+json, xem quyết định 9
    else thành công
      ID-->>C: 201 id, email, roles, không trả password_hash
    end
  end
```
- Role mặc định `ATTENDEE` là **ĐỀ XUẤT** (source không nêu); client **không** được gửi `roles` (mass assignment).
- Không tự động đăng nhập sau đăng ký (đề xuất, giữ luồng đơn giản).
- **Bất biến:** email duy nhất không phân biệt hoa/thường (chặn bằng DB, không chỉ bằng code); không bao giờ log hoặc trả mật khẩu/hash.

### PH1-D6 — Login
```mermaid
sequenceDiagram
  autonumber
  actor C as Client
  participant GW as Gateway
  participant ID as identity
  participant DB as identity-db
  C->>GW: POST login với email và password
  GW->>GW: rate limit theo IP
  alt vượt ngưỡng
    GW-->>C: 429 kèm Retry-After
  else còn trong ngưỡng
    GW->>ID: chuyển tiếp
    ID->>DB: tìm user theo lower email
    alt user không tồn tại
      ID->>ID: vẫn chạy 1 phép hash giả để giảm chênh lệch thời gian
      ID-->>C: 401 thông báo chung
    else user tồn tại
      ID->>ID: kiểm status LOCKED hoặc locked_until còn hiệu lực
      alt đang bị khóa
        ID-->>C: 401 thông báo chung, hoặc mã riêng theo quyết định
      else được phép thử
        ID->>ID: verify mật khẩu với hash
        alt sai mật khẩu
          ID->>DB: tăng failed_login_count, có thể đặt locked_until
          ID-->>C: 401 thông báo chung
        else đúng
          ID->>DB: reset bộ đếm, INSERT refresh_tokens với family_id mới
          ID->>ID: tạo access JWT
          ID-->>C: 200 accessToken, expiresIn, cùng Set-Cookie refresh token
        end
      end
    end
  end
```
- **Thông báo chung** cho "không có user" và "sai mật khẩu" để tránh dò email (user enumeration).
- Thông tin lock (`failed_login_count`, `locked_until`) là **đề xuất schema** (quyết định 6).
- Access token trả trong body để client giữ **trong memory**; refresh token chỉ đi qua cookie `httpOnly` (S07).

## Mục 4. JWT RS256 và JWKS verification

### PH1-D7 — Phát hành và verify
```mermaid
sequenceDiagram
  autonumber
  participant ID as identity
  participant GW as Gateway
  participant C as Client
  Note over ID: khởi động: nạp khóa RSA private từ secret ngoài repo, gán kid
  GW->>ID: GET JWKS khi khởi động hoặc cache hết TTL
  ID-->>GW: public key theo kid, không có phần private
  GW->>GW: cache JWKS theo TTL
  C->>GW: request kèm Bearer access token
  GW->>GW: đọc header kid, chỉ chấp nhận alg RS256
  alt kid không có trong cache
    GW->>ID: refetch JWKS tối đa 1 lần, có giới hạn tần suất
    ID-->>GW: JWKS mới
  end
  GW->>GW: verify chữ ký, exp, iss, aud
  alt hợp lệ
    GW->>GW: kiểm role thô, gắn user context
  else không hợp lệ
    GW-->>C: 401 problem+json
  end
```

**Claims của access token**
| Claim | Nguồn | Ghi chú |
|---|---|---|
| `sub` | source | userId |
| `roles` | source | danh sách role |
| `jti` | source | id token; dùng nếu bật denylist (OPTIONAL) |
| `exp`, `iat` | đề xuất | `exp` = `iat` + TTL access |
| `iss`, `aud` | đề xuất | để từ chối token do bên khác phát hoặc dành cho hệ khác |
| header `kid`, `alg=RS256` | đề xuất | `kid` cần để hỗ trợ xoay khóa |

- Vì sao **RS256 + JWKS**: chỉ identity giữ khóa **private** (ký); gateway và mọi service chỉ cần khóa **public** để verify, nên lộ một service không cho phép giả token. HS256 dùng chung bí mật nên không có tính chất này.
- **Phải ép `alg`**: không cho token tự chọn thuật toán (chặn `alg=none` và nhầm lẫn HS256/RS256).
- **Optimization (source):** cache JWKS có TTL; đo chi phí verify ở gateway.
- **SOURCE CHƯA QUY ĐỊNH:** đường dẫn JWKS, TTL cache, quy trình xoay khóa, độ lệch đồng hồ cho phép (clock skew).

## Mục 5. Vòng đời access token và refresh token

### PH1-D8 — Dòng thời gian một phiên
```mermaid
flowchart LR
  L["Login"] --> A1["Access A1<br/>10–15 phút, trong memory"]
  L --> R1["Refresh R1<br/>7–14 ngày, cookie httpOnly<br/>family F1"]
  A1 -->|"hết hạn"| RF1["Refresh bằng R1"]
  RF1 --> A2["Access A2"]
  RF1 --> R2["Refresh R2, R1 bị revoke<br/>cùng family F1"]
  R2 -->|"lặp lại"| RFN["Rotation tiếp"]
  R2 -->|"quá hạn hoặc logout"| END["Hết phiên, phải login lại"]
```

### PH1-D9 — Trạng thái của một refresh token
> **ĐỀ XUẤT BỔ SUNG:** tên state là cách mô hình hóa từ các cột `revoked_at`, `replaced_by`, `expires_at` (S05); source không có state machine này.
```mermaid
stateDiagram-v2
  [*] --> ACTIVE : cấp khi login hoặc rotate
  ACTIVE --> ROTATED : refresh thành công, set revoked_at và replaced_by
  ACTIVE --> LOGGED_OUT : logout, set revoked_at
  ACTIVE --> EXPIRED : quá expires_at
  ROTATED --> FAMILY_REVOKED : bị dùng lại, reuse detected
  ACTIVE --> FAMILY_REVOKED : cả family bị thu hồi
  FAMILY_REVOKED --> [*]
  EXPIRED --> [*]
  LOGGED_OUT --> [*]
```
- Access token **không** lưu ở DB; hiệu lực chỉ do `exp` và chữ ký.
- Refresh token là chuỗi ngẫu nhiên 256-bit (opaque); DB chỉ lưu **hash** (`token_hash` UNIQUE). Vì token đã có entropy cao, một hàm hash nhanh như SHA-256 là đủ (đề xuất), không cần hash chậm như với mật khẩu.
- **Phải revoke chứ không xóa dòng**: nếu xóa thì mất khả năng phát hiện reuse.

## Mục 6. Refresh rotation và reuse detection

### PH1-D10 — Refresh
```mermaid
sequenceDiagram
  autonumber
  actor C as Client
  participant ID as identity
  participant DB as identity-db
  C->>ID: POST refresh kèm cookie R1 và Origin
  ID->>ID: kiểm Origin hoặc header tùy biến, chống CSRF
  ID->>ID: hash R1
  ID->>DB: SELECT refresh_tokens theo token_hash
  alt không có hoặc đã quá hạn
    ID-->>C: 401 và xóa cookie
  else đã revoked hoặc đã rotated, tức dùng lại token cũ
    ID->>DB: UPDATE revoke mọi token chưa revoke của family
    ID-->>C: 401, xóa cookie, ghi log sự kiện bảo mật
  else đang ACTIVE
    ID->>DB: kiểm user còn ACTIVE, đọc lại roles hiện tại
    ID->>DB: UPDATE revoked_at, replaced_by WHERE id = R1 AND revoked_at IS NULL
    alt 0 dòng bị ảnh hưởng, request khác đã rotate trước
      ID->>DB: xử lý như reuse hoặc grace window, theo quyết định 7
      ID-->>C: 401
    else đúng 1 dòng
      ID->>DB: INSERT R2 cùng family_id, expires_at mới
      ID->>ID: tạo access token mới với roles mới đọc
      ID-->>C: 200 access mới và Set-Cookie R2
    end
  end
```
- **Atomic:** việc "revoke R1" phải là **một câu UPDATE có điều kiện** và kiểm số dòng bị ảnh hưởng, không phải SELECT rồi UPDATE ở tầng ứng dụng (cùng bài học chống race ở Phase 4). Hai request đồng thời dùng cùng R1 thì chỉ một cái thắng.
- Mỗi lần refresh đọc lại roles/status từ DB ⇒ đây là chỗ role mới và lock tài khoản có hiệu lực.
- Request refresh song song từ 2 tab có thể bị coi là reuse; giải pháp phía client là **single-flight** (S08-D3).

### PH1-D11 — Family và reuse detection
```mermaid
flowchart LR
  subgraph FAM["family_id = F1"]
    R1["R1 revoked, replaced_by R2"] --> R2["R2 revoked, replaced_by R3"] --> R3["R3 ACTIVE"]
  end
  ATK["Kẻ trộm replay R1"] -->|"R1 đã revoked: reuse detected"| REV["Revoke toàn bộ F1, gồm cả R3"]
  REV --> UL["Người dùng thật phải login lại"]
```
- Lý do: nếu token cũ xuất hiện lại thì có hai bên cùng nắm một chuỗi token; không biết bên nào là thật nên thu hồi cả chuỗi.
- **Trade-off:** người dùng thật bị đăng xuất khi có kẻ trộm replay (và khi có race khi refresh song song).

### PH1-D12 — Cookie, CORS và CSRF
```mermaid
sequenceDiagram
  autonumber
  participant B as Browser trên origin A
  participant GW as Gateway
  participant ID as identity
  participant X as Trang tấn công trên origin B
  B->>GW: OPTIONS preflight kèm Origin A
  GW-->>B: Allow-Origin A, Allow-Credentials true, không dùng wildcard
  B->>GW: POST refresh kèm cookie và header tùy biến
  GW->>ID: chuyển tiếp
  ID->>ID: Origin thuộc allowlist và có header tùy biến thì cho qua
  ID-->>B: 200 và Set-Cookie mới
  X->>GW: POST refresh từ origin B
  Note over X,GW: SameSite=Strict nên trình duyệt không gửi cookie
  GW->>ID: nếu vẫn tới được
  ID-->>X: 403 vì Origin không hợp lệ hoặc thiếu header tùy biến
```
- Cookie refresh: `HttpOnly; Secure; SameSite=Strict; Path` **khớp đường dẫn refresh**; chỉ dùng cho refresh/logout.
- `SameSite=Strict` chỉ giúp khi web và API cùng *site* (cùng tên miền đăng ký, ví dụ khác subdomain). Nếu khác site hoàn toàn thì cookie không gửi được: cần thiết kế deploy hợp lý (S21).
- Không dùng `Allow-Origin: *` cùng `Allow-Credentials: true`.

## Mục 7. Logout và revocation

### PH1-D13 — Logout
```mermaid
flowchart TB
  LO["POST logout kèm cookie refresh"] --> H["Hash token, tìm trong DB"]
  H --> Q{"Tìm thấy?"}
  Q -->|"có"| RV["Revoke token và family"]
  Q -->|"không"| NOP["Không làm gì, vẫn trả thành công: idempotent"]
  RV --> CK["Xóa cookie: Set-Cookie Max-Age=0, cùng Path và thuộc tính"]
  NOP --> CK
  CK --> OK["204"]
  OK --> AT["Access token hiện có vẫn hợp lệ đến khi hết hạn, tối đa khoảng 15 phút"]
  AT --> DL{"Cần chặn ngay?"}
  DL -->|"có"| DN["Denylist jti trong Redis: OPTIONAL, ngoài Phase 1"]
  DL -->|"không"| W["Chấp nhận trade-off của JWT stateless"]
```
- Logout "thật sự làm gì" trong hệ JWT stateless: **chỉ chặn việc tạo access token mới** (refresh), không thu hồi access token đang sống.
- Phải ghi rõ trade-off này vào ADR và `docs/learning` (câu hỏi phỏng vấn của Phase 1).

## Mục 8. RBAC và method-level authorization

### PH1-D14 — Từ DB đến quyết định cho phép
```mermaid
flowchart LR
  UR[("user_roles trong DB")] --> JWT["Claim roles trong access token"]
  JWT --> GWC["Gateway: role thô theo route, ví dụ admin cần ADMIN"]
  GWC --> SVC["Service: verify lại JWT"]
  SVC --> MS["Method security: PreAuthorize theo role"]
  MS --> OWN["Ownership check: từ Phase 2 trở đi"]
```

### PH1-D15 — Quyết định khi ADMIN đổi role
```mermaid
flowchart TB
  REQ["Yêu cầu đổi role của user X"] --> G{"Gateway: có role ADMIN?"}
  G -->|"không"| D1["403"]
  G -->|"có"| V{"identity verify lại JWT hợp lệ?"}
  V -->|"không"| D2["401"]
  V -->|"có"| M{"Method security: hasRole ADMIN?"}
  M -->|"không"| D3["403"]
  M -->|"có"| R{"Role hợp lệ và X tồn tại?<br/>không cho SYSTEM hoặc GUEST<br/>không để hệ thống hết ADMIN"}
  R -->|"không"| D4["400 hoặc 409"]
  R -->|"có"| TX["Cập nhật user_roles trong 1 transaction"]
  TX --> LG["Ghi structured log hành động: ai, đổi gì, cho ai<br/>audit-service có từ Phase 7"]
  LG --> OK["200"]
```
- Role dùng trong Phase 1: `ATTENDEE`, `ORGANIZER`, `EVENT_STAFF`, `MODERATOR`, `ADMIN` (S01). `GUEST` không lưu DB; `SYSTEM` là service account, chưa dùng ở Phase 1 (xem mục 19).
- Ánh xạ claim `roles` sang authority của Spring (thêm tiền tố `ROLE_`) là chỗ hay sai: kiểm bằng test.
- Role không bao giờ lấy từ body hoặc header do client gửi.
- Đổi role chỉ ADMIN và **phải được ghi lại** (S07).

## Mục 9. Organizer profile

### PH1-D16 — Trạng thái hồ sơ organizer
```mermaid
stateDiagram-v2
  [*] --> PENDING : user nộp hồ sơ
  PENDING --> VERIFIED : ADMIN approve, ghi verified_by và verified_at
  PENDING --> REJECTED : ADMIN reject
  REJECTED --> PENDING : nộp lại, ĐỀ XUẤT
  VERIFIED --> [*]
```
### PH1-D17 — Nộp và duyệt
```mermaid
sequenceDiagram
  autonumber
  actor U as ATTENDEE
  actor A as ADMIN
  participant ID as identity
  participant DB as identity-db
  U->>ID: nộp hồ sơ với org_name
  ID->>DB: INSERT organizer_profiles PENDING, kiểm chưa có hồ sơ đang mở
  ID-->>U: 201 trạng thái PENDING
  A->>ID: xem danh sách PENDING
  A->>ID: approve hoặc reject user U
  ID->>DB: 1 transaction: cập nhật status, verified_by, verified_at và với approve thêm role ORGANIZER
  ID-->>A: 200
  Note over U,ID: access token cũ của U chưa có role ORGANIZER cho đến khi refresh
```
- Source chỉ cho: bảng `organizer_profiles(user_id, org_name, status PENDING|VERIFIED|REJECTED, verified_by, verified_at)` và "ADMIN verify". Phần còn lại (nộp lại, lý do từ chối, thời điểm cấp role ORGANIZER) là **đề xuất / quyết định 5**.
- Ràng buộc nghiệp vụ liên quan Phase 2: submit sự kiện yêu cầu organizer đã VERIFIED (S04); **không** kiểm ở Phase 1.
- ADMIN không tự verify cho chính mình nếu là organizer (separation of duties, S01): ĐỀ XUẤT.

## Mục 10. Rate limit login

### PH1-D18 — Hai lớp chặn
```mermaid
flowchart TB
  REQ["Request login hoặc register"] --> L1{"Lớp 1 gateway: token bucket theo IP trong Redis còn token?"}
  L1 -->|"hết"| R429["429 kèm Retry-After"]
  L1 -->|"còn"| ID["identity xử lý"]
  ID --> L2{"Lớp 2: tài khoản đang bị khóa tạm?"}
  L2 -->|"có"| DENY["Từ chối với thông báo chung"]
  L2 -->|"không"| CHK["Verify mật khẩu"]
  CHK -->|"sai"| INC["Tăng bộ đếm, đủ N lần thì đặt locked_until"]
  CHK -->|"đúng"| RST["Reset bộ đếm"]
```
- Hai lớp bổ trợ nhau: IP chặn tấn công **một nguồn nhiều tài khoản**; khóa tạm theo tài khoản chặn **nhiều nguồn một tài khoản**.
- **Trade-off:** khóa tài khoản cho phép kẻ xấu cố ý khóa người khác ⇒ khóa chỉ **tạm thời**, thông báo chung, và kết hợp lớp IP.
- **SOURCE CHƯA QUY ĐỊNH:** dung lượng bucket, tốc độ nạp, N, thời gian khóa; bạn đặt giá trị, đưa vào cấu hình và ghi lý do.

## E. Những điều phải nhớ (toàn file)
- Mọi lỗi auth dùng cùng error model problem+json của Phase 0; không lộ stack trace, không phân biệt "sai email" và "sai mật khẩu".
- Không log: mật khẩu, access token, refresh token, hash; userId nên được băm trong log (S09).
- Mọi giá trị chưa chốt ở source phải thành **cấu hình** + ghi trong ADR.
