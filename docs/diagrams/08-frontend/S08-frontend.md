# S08 — Frontend (Next.js + React + TypeScript)

> Nguồn: mục "8. FRONTEND" (8.1 Page structure, 8.2 Kỹ thuật cần thể hiện).

## A. Mục tiêu
- Thiết kế UI vừa đủ nhưng giao tiếp với backend như hệ thống thật: auth, refresh, idempotency, SSE, xử lý lỗi.
- Liên hệ: S01 (role/trang), S07 (token), S04 (checkout), Phase 3 (frontend nền), Phase 7–8 (SSE).

## B. Overview diagram

### S08-D1 — Sitemap theo role
```mermaid
flowchart LR
  ROOT["Next.js App Router"] --> PUB["Công khai"]
  ROOT --> ATT["Attendee"]
  ROOT --> ORG["Organizer"]
  ROOT --> STF["Staff"]
  ROOT --> MOD["Moderator"]
  ROOT --> ADM["Admin"]
  PUB --> P1["/ Landing + sự kiện nổi bật SSR/ISR"]
  PUB --> P2["/events list: search, filter city/date/price, cursor pagination"]
  PUB --> P3["/events/slug chi tiết + tier + nút Mua hoặc Vào phòng chờ"]
  PUB --> P4["/auth/login, /auth/register"]
  ATT --> A1["/queue/eventId phòng chờ: vị trí, ETA, SSE"]
  ATT --> A2["/checkout/orderId countdown hold 10p, thanh toán, trạng thái order"]
  ATT --> A3["/me/orders, /me/tickets vé QR"]
  ORG --> O1["/organizer/events danh sách, tạo, sửa, submit duyệt"]
  ORG --> O2["/organizer/events/id/tiers, /staff, /dashboard real-time"]
  STF --> S1["/staff/checkin/eventId quét QR, lịch sử quét"]
  MOD --> M1["/moderator/queue hàng chờ duyệt"]
  ADM --> D1["/admin/users, /audit, /refunds, /dlq"]
```

## C. Detailed diagrams

### S08-D2 — Kỹ thuật cần thể hiện
```mermaid
mindmap
  root((Frontend))
    API client
      Wrapper typed fetch + zod
      Tự gắn token
      Tự refresh khi 401 single-flight
      Gắn X-Correlation-Id
    Auth
      Access token trong memory
      Middleware Next.js chặn route theo role
      Check role ở UI chỉ để UX
    State
      TanStack Query cho server state
      Zustand hoặc Context cho UI state nhỏ
      Không Redux
    Form
      react-hook-form + zod
      Hiển thị lỗi field từ backend errors
    Error và Loading
      Error boundary
      Skeleton và retry
      Phân biệt 4xx 5xx 429
    Idempotency
      Sinh Idempotency-Key một lần cho mỗi ý định mua
      Disable nút khi gửi
    Real-time
      SSE cho queue order dashboard
      Tự reconnect với Last-Event-ID
    Responsive
      Tailwind mobile-first
      Staff check-in dùng điện thoại
```

### S08-D3 — Silent refresh single-flight
```mermaid
sequenceDiagram
  autonumber
  participant UI as Components
  participant API as API client
  participant GW as Gateway
  participant ID as identity
  UI->>API: request A
  UI->>API: request B
  API->>GW: A kèm access token hết hạn
  API->>GW: B kèm access token hết hạn
  GW-->>API: 401 cho A
  GW-->>API: 401 cho B
  API->>API: chỉ 1 refresh đang chạy, B đợi chung kết quả
  API->>ID: refresh bằng cookie
  alt refresh thành công
    ID-->>API: access token mới
    API->>GW: gửi lại A
    API->>GW: gửi lại B
    GW-->>UI: kết quả
  else refresh thất bại
    ID-->>API: 401
    API-->>UI: xóa phiên, chuyển về login
  end
```

### S08-D4 — Bảo vệ route
```mermaid
flowchart TB
  NAV["Điều hướng đến route"] --> MW["Middleware Next.js"]
  MW --> T{"Có phiên hợp lệ?"}
  T -->|"không"| L["Chuyển về /auth/login"]
  T -->|"có"| R{"Role phù hợp route?"}
  R -->|"không"| F["Trang 403"]
  R -->|"có"| PG["Render trang"]
  PG --> API["Gọi API"]
  API --> BE["Backend vẫn kiểm quyền thật"]
  BE -->|"403"| F
  Note1["Chặn ở UI chỉ để trải nghiệm tốt hơn, không phải bảo mật"] -.-> MW
```

### S08-D5 — Idempotency-Key trong ý định mua
```mermaid
flowchart TB
  I["User bấm Mua"] --> K["Sinh Idempotency-Key một lần cho ý định này"]
  K --> S["Gửi POST /orders, disable nút, hiện đang giữ vé"]
  S --> R{"Kết quả?"}
  R -->|"thành công"| OK["Chuyển sang /checkout/orderId"]
  R -->|"timeout hoặc 5xx"| RT["Retry với CÙNG key"]
  RT --> S
  R -->|"4xx lỗi nhập hoặc hết vé"| ER["Hiện lỗi cho user sửa"]
  R -->|"429"| W["Chờ theo Retry-After"]
  ER --> NEW["User bấm Mua lần mới thì key mới"]
```

### S08-D6 — SSE: vị trí hàng đợi và trạng thái order
```mermaid
sequenceDiagram
  autonumber
  participant B as Browser
  participant GW as Gateway
  participant WR as waiting-room hoặc order
  B->>GW: mở SSE stream
  GW->>WR: chuyển tiếp
  WR-->>B: event kèm id, vị trí hoặc trạng thái
  WR-->>B: heartbeat định kỳ
  Note over B,WR: mất kết nối
  B->>GW: mở lại với Last-Event-ID
  GW->>WR: chuyển tiếp
  WR-->>B: gửi bù từ sau id đó
  WR-->>B: được admit thì UI chuyển sang checkout
  Note over WR: chỉ user đó được nghe stream của mình, tránh rò quyền
```

### S08-D7 — Hành trình người mua trên UI
```mermaid
flowchart LR
  E["Chi tiết sự kiện"] --> Q{"Waiting room đang bật?"}
  Q -->|"có"| JQ["Vào /queue/eventId"]
  JQ -->|"SSE: được admit"| CO["/checkout/orderId"]
  Q -->|"không"| CO
  CO --> CD["Countdown hold 10 phút"]
  CD --> PAY["Thanh toán mock"]
  PAY --> ST["Theo dõi trạng thái order qua SSE hoặc poll"]
  ST -->|"COMPLETED"| TK["/me/tickets vé QR"]
  ST -->|"EXPIRED hoặc lỗi"| E
```

### S08-D8 — Xử lý lỗi theo nhóm mã
```mermaid
flowchart LR
  ERR["Response lỗi"] --> G{"Nhóm"}
  G -->|"4xx"| U["User tự sửa, hiện lỗi từng field"]
  G -->|"401"| RF["Thử refresh một lần"]
  G -->|"409"| RC["Refetch dữ liệu và thử lại có giới hạn"]
  G -->|"429"| WT["Chờ Retry-After"]
  G -->|"5xx"| TR["Cho phép thử lại, retry với backoff"]
  G -->|"render lỗi"| EB["Error boundary"]
```

## D. Giải thích sơ đồ
- **D1** chia trang theo role; mỗi trang tương ứng một nhóm API ở backend.
- **D3** "single-flight" tránh việc nhiều request cùng 401 rồi cùng gọi refresh (với rotation + reuse detection, refresh song song có thể bị coi là reuse và làm mất phiên).
- **D5** Idempotency-Key gắn với **ý định mua**, không phải mỗi lần gửi request: retry dùng lại key, ý định mới dùng key mới.
- **D6** SSE một chiều server→client là đủ (ADR-007); reconnect bằng `Last-Event-ID`.
- Không optimistic UI cho inventory: UI chỉ hiển thị "đang giữ vé…"; quyết định thật ở inventory.

## E. Những điều phải nhớ
- Mục tiêu là giao tiếp backend thật, không phải animation; UI không quá 25% thời gian (S22).
- Check quyền ở frontend chỉ để UX.
- Khi dữ liệu eventual, UI phải có trạng thái trung gian rõ ràng.
- **SOURCE CHƯA QUY ĐỊNH:** mã lỗi và schema `errors[]` cụ thể (source chỉ nói hiển thị lỗi field từ backend và error model problem+json ở S13), chiến lược polling thay thế khi SSE không dùng được, giao diện quét QR (thư viện camera), kích thước trang cursor, cách chọn ETA trong phòng chờ.

## F. Liên kết
- Đầu vào: S01, S04, S07. Đầu ra: Phase 3 (UI nền), Phase 7 (SSE), Phase 8 (/queue), Phase 11 (landing + simulator).

## G. Checklist
- [x] 8.1 page structure  - [x] 8.2 API client, auth, state, form, error/loading, idempotency, real-time, optimistic, responsive
