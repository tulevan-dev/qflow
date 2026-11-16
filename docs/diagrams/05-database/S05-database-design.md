# S05 — Database Design (5.1–5.8)

> Nguồn: mục "5. DATABASE DESIGN (mỗi service 1 DB riêng)".

## A. Mục tiêu
- Mô tả schema, ràng buộc, index và **quyền sở hữu dữ liệu** của từng service, kèm lý do thiết kế.
- Giải thích vì sao không có cross-service FK và vì sao service không đọc DB của nhau.
- Liên hệ: S03 (ownership), S04 (state/hold/outbox cần cột nào), S06 (outbox/inbox), S07 (refresh token family).

## B. Overview diagram

### S05-D1 — Database ownership
```mermaid
flowchart LR
  ID["identity"] --> D1[("identity-db")]
  CAT["catalog"] --> D2[("catalog-db")]
  INV["inventory"] --> D3[("inventory-db")]
  ORD["order"] --> D4[("order-db")]
  PAY["payment"] --> D5[("payment-db")]
  TKT["ticket"] --> D6[("ticket-db")]
  NOTI["notification"] --> D7[("notification-db")]
  AUD["audit"] --> D8[("audit-db")]
  WR["waiting-room"] --> R1[("Redis")]
  GW["gateway"] --> R2[("Redis rate limit")]
  X["Service khác"] -.->|"CẤM đọc DB trực tiếp, CẤM cross-service FK"| D3
```
Quy ước chung: `id uuid` (UUIDv7 nếu thư viện hỗ trợ), `created_at`, `updated_at`, `version bigint` ở bảng có cạnh tranh; tiền lưu `bigint` đơn vị nhỏ nhất; dùng **Flyway** cho mọi migration; dev dùng 1 Postgres nhiều database nhưng tách logic nghiêm ngặt.

## C. Detailed diagrams
> Quan hệ **giữa các service** chỉ là ID tham chiếu (không FK), nên ER dưới đây chỉ vẽ quan hệ **trong** từng service; cột tham chiếu ngoài được chú thích.

### S05-D2 — identity-db (5.1)
```mermaid
erDiagram
  users ||--o{ user_roles : has
  users ||--o| organizer_profiles : has
  users ||--o{ refresh_tokens : owns
  users {
    uuid id PK
    string email UK "unique trên lower email"
    string password_hash
    string status "ACTIVE LOCKED DELETED"
    timestamp created_at
    timestamp updated_at
    timestamp deleted_at "soft delete"
  }
  user_roles {
    uuid user_id PK, FK
    string role PK
  }
  organizer_profiles {
    uuid user_id PK, FK
    string org_name
    string status "PENDING VERIFIED REJECTED"
    uuid verified_by
    timestamp verified_at
  }
  refresh_tokens {
    uuid id PK
    uuid user_id FK
    string token_hash UK "lưu hash không lưu plaintext"
    uuid family_id "phát hiện token reuse"
    timestamp expires_at
    timestamp revoked_at
    uuid replaced_by
    string user_agent
    string ip
    timestamp created_at
  }
```

### S05-D3 — catalog-db (5.2)
```mermaid
erDiagram
  events ||--o{ ticket_tiers : has
  events ||--o{ event_reviews : reviewed_by
  events ||--o{ event_status_history : logs
  events ||--o{ event_staff : assigns
  events {
    uuid id PK
    uuid organizer_id "ref identity không FK"
    string title
    string slug UK
    string description
    string venue
    string city
    timestamp starts_at
    timestamp ends_at
    timestamp sale_starts_at
    timestamp sale_ends_at
    string status
    tsvector search_vector "GIN full-text"
    bigint version
    timestamp created_at
    timestamp updated_at
    timestamp deleted_at
  }
  ticket_tiers {
    uuid id PK
    uuid event_id FK
    string name "UK cùng event_id"
    bigint price_amount "đơn vị nhỏ nhất"
    string currency
    int total_quantity
    int max_per_order
    int max_per_user
    int sort_order
    bigint version
  }
  event_reviews {
    uuid id PK
    uuid event_id FK
    uuid reviewer_id
    string decision
    string reason
    timestamp created_at
  }
  event_status_history {
    uuid id PK
    uuid event_id FK
    string from_status
    string to_status
    uuid actor_id
    string actor_role
    timestamp created_at
  }
  event_staff {
    uuid event_id PK, FK
    uuid staff_id PK
    uuid assigned_by
  }
  outbox_events {
    uuid id PK
    timestamp published_at "chỉ nhắc ở mục Observability"
    string other_columns "SOURCE CHƯA QUY ĐỊNH"
  }
```

### S05-D4 — inventory-db (5.3)
```mermaid
erDiagram
  tier_stock ||--o{ holds : reserves
  tier_stock {
    uuid tier_id PK
    uuid event_id
    int total
    int reserved
    int sold
    bigint version
  }
  holds {
    uuid id PK
    uuid order_id "UK cùng tier_id"
    uuid tier_id FK
    uuid user_id
    int quantity
    string status "ACTIVE CONSUMED RELEASED EXPIRED"
    timestamp expires_at
    timestamp created_at
  }
  user_tier_purchases {
    uuid user_id PK
    uuid tier_id PK
    int quantity
  }
  processed_events {
    uuid event_id PK
    string consumer
    timestamp processed_at
  }
  outbox_events {
    uuid id PK
    string other_columns "SOURCE CHƯA QUY ĐỊNH"
  }
```

### S05-D5 — order-db (5.4)
```mermaid
erDiagram
  orders ||--o{ order_items : contains
  orders ||--o{ order_status_history : logs
  orders {
    uuid id PK
    uuid user_id
    uuid event_id
    string status
    bigint total_amount
    string currency
    string idempotency_key "UK cùng user_id"
    timestamp expires_at
    bigint version
    timestamp created_at
    timestamp updated_at
  }
  order_items {
    uuid id PK
    uuid order_id FK
    uuid tier_id
    string tier_name "snapshot"
    bigint unit_price "snapshot"
    int quantity
  }
  order_status_history {
    uuid id PK
    string columns "SOURCE CHƯA QUY ĐỊNH chi tiết cột"
  }
  outbox_events {
    uuid id PK
    string other_columns "SOURCE CHƯA QUY ĐỊNH"
  }
  processed_events {
    uuid event_id PK
    string consumer
  }
```

### S05-D6 — payment-db (5.5)
```mermaid
erDiagram
  payments ||--o{ refunds : refunded_by
  payments {
    uuid id PK
    uuid order_id "UK per active payment"
    bigint amount
    string currency
    string status "CREATED PENDING SUCCEEDED FAILED REFUNDED"
    string provider
    string provider_txn_id UK
    string idempotency_key UK
    bigint version
  }
  webhook_events {
    string provider_event_id PK "chống xử lý webhook trùng"
    jsonb payload "raw payload để debug tranh chấp"
    boolean signature_valid
    timestamp received_at
    timestamp processed_at
  }
  refunds {
    uuid id PK
    uuid payment_id FK
    bigint amount
    string status
    string reason
    string idempotency_key UK
  }
  outbox_events {
    uuid id PK
    string other_columns "SOURCE CHƯA QUY ĐỊNH"
  }
  processed_events {
    uuid event_id PK
    string consumer
  }
```

### S05-D7 — ticket-db (5.6)
```mermaid
erDiagram
  tickets ||--o{ check_in_logs : scanned
  tickets {
    uuid id PK
    uuid order_id
    uuid event_id
    uuid tier_id
    uuid owner_id
    string code UK
    string status "ISSUED CHECKED_IN VOID"
    timestamp issued_at
    timestamp checked_in_at
    uuid checked_in_by
    bigint version
    int seq "suy ra từ UK order_id tier_id seq"
  }
  check_in_logs {
    uuid id PK
    uuid ticket_id FK
    uuid staff_id
    string result "OK DUPLICATE INVALID WRONG_EVENT"
    timestamp scanned_at
  }
  event_staff_replica {
    uuid event_id
    uuid staff_id
  }
  event_replica {
    uuid event_id PK
    timestamp starts_at
    timestamp ends_at
    string status
  }
```
`event_staff_replica` và `event_replica` là **local read model** cập nhật từ event của catalog.

### S05-D8 — notification-db và audit-db (5.7)
```mermaid
erDiagram
  notifications ||--o{ deliveries : delivered_by
  notifications {
    uuid id PK
    uuid user_id
    string type
    string title
    string body
    jsonb payload
    timestamp read_at
    timestamp created_at
  }
  deliveries {
    uuid id PK
    uuid notification_id FK
    string channel
    string status
    int attempts
    string last_error
    timestamp next_retry_at
    uuid source_event_id "UK cùng channel"
  }
  audit_logs {
    uuid id PK
    timestamp occurred_at
    uuid actor_id
    string actor_role
    string action
    string resource_type
    string resource_id
    string correlation_id
    jsonb payload
  }
```
`audit_logs` append-only: REVOKE UPDATE/DELETE; partition theo tháng là mức ADVANCED.

### S05-D9 — Redis dùng cho gì (5.8)
```mermaid
flowchart LR
  WR["waiting-room"] --> K1["wr:eventId:queue<br/>ZSET score = join time"]
  WR --> K2["wr:eventId:admitted:userId<br/>TTL, đã được vào"]
  GW["gateway"] --> K3["rl:route:userId/ip<br/>rate limit"]
  CAT["catalog"] --> K4["cache:event:id<br/>cache"]
  INV["inventory"] --> K5["avail:tierId<br/>cache hiển thị, KHÔNG quyết định"]
  NOTI["notification"] -.-> K6["Redis pub/sub<br/>SSE push"]
  K5 -.->|"quyết định thật luôn ở PostgreSQL"| PG[("PG inventory")]
```

### Bảng ràng buộc và index được source quy định (bổ trợ cho ER)
| DB | Ràng buộc / index | Lý do |
|---|---|---|
| identity | `INDEX(user_id)`, `INDEX(family_id)` trên refresh_tokens; email unique trên `lower(email)` | tra token theo user/family; tránh trùng hoa-thường |
| catalog | `INDEX(status, starts_at)`; `INDEX(organizer_id, status)`; `GIN(search_vector)`; `UNIQUE(event_id, name)` trên tier | list công khai; dashboard organizer; full-text |
| inventory | `CHECK(reserved >= 0 AND sold >= 0 AND reserved + sold <= total)`; `UNIQUE(order_id, tier_id)`; partial `INDEX(expires_at) WHERE status='ACTIVE'` | lớp phòng thủ cuối chống oversell; reserve idempotent; index nhỏ cho expiry worker |
| order | `UNIQUE(user_id, idempotency_key)`; `INDEX(user_id, created_at DESC)`; `INDEX(status, expires_at)` | idempotency; list order; reconciler tìm order kẹt |
| payment | `provider_event_id` PK; `provider_txn_id` UNIQUE; `idempotency_key` UNIQUE | chống webhook trùng ở tầng DB |
| ticket | `UNIQUE(order_id, tier_id, seq)`; `INDEX(event_id, status)` | phát hành idempotent |
| notification | `UNIQUE(source_event_id, channel)`; `INDEX(user_id, created_at DESC)` | không gửi trùng |
| audit | append-only, partition theo tháng (ADVANCED) | không ai sửa được log |

## D. Giải thích sơ đồ
- **D1:** mỗi service có DB riêng; nếu service A đọc bảng của B thì schema của B bị đóng băng, mất deploy độc lập và thành distributed monolith.
- **D2:** lưu **hash** refresh token (lộ DB không lộ token); `family_id` + `replaced_by` phục vụ phát hiện token reuse (S07). Soft delete cho user vì audit/order cần tham chiếu.
- **D3:** `price_amount` chỉ là giá hiện tại; order lưu snapshot riêng. `event_status_history` phục vụ audit/tranh chấp.
- **D4:** `CHECK` là lớp cuối chống oversell kể cả khi code sai; `UNIQUE(order_id, tier_id)` làm reserve idempotent; partial index chỉ index hold `ACTIVE`.
- **D5–D6:** `idempotency_key` + unique chặn tạo trùng; snapshot tên/giá trong `order_items`; `webhook_events` PK chặn xử lý trùng và giữ raw payload để debug.
- **D7:** replica cho phép check-in tại cổng mà **không gọi catalog**, đánh đổi bằng eventual consistency.
- **D9:** Redis chỉ để tăng tốc/điều tiết; **không** là source of truth tồn kho ở v1.

## E. Những điều phải nhớ
- Không cross-service FK; chỉ lưu ID.
- Mọi migration qua Flyway và phải backward compatible (S21: expand → migrate → contract).
- Tiền không dùng float.
- **SOURCE CHƯA QUY ĐỊNH:** cột chi tiết của `outbox_events` ở mọi service; cột của `order_status_history`; `processed_events` ở ticket/notification/audit (source chỉ nêu rõ ở inventory, order, payment dù các consumer khác cũng bắt buộc idempotent); `outbox_events` ở ticket (ticket phát `TicketsIssued` nhưng 5.6 không liệt kê bảng outbox); cột `seq` của tickets chỉ xuất hiện trong UNIQUE; schema chi tiết cho waiting-room Postgres (source nói "Redis, + Postgres nhỏ nếu cần").

## F. Liên kết
- Đầu vào: S03 (ownership), S04 (state, hold). Đầu ra: S06 (outbox/inbox), S07 (refresh_tokens), Phase 1–6 (migration đầu tiên mỗi service).

## G. Checklist
- [x] 5.1 identity  - [x] 5.2 catalog  - [x] 5.3 inventory  - [x] 5.4 order  - [x] 5.5 payment  - [x] 5.6 ticket  - [x] 5.7 notification + audit  - [x] 5.8 Redis  - [x] ownership + lý do thiết kế
