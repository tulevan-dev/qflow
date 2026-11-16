# QFLOW — PROJECT BLUEPRINT

> Nền tảng bán vé sự kiện cộng đồng **có phòng chờ công bằng (fair waiting room)** — chống bot, chống oversell, chống "F5 giành vé". Build theo hướng **microservices từ đầu**, event-driven, dành cho người đã có kiến thức nền muốn nhảy từ Fresher → Junior vững.

------------------------------------------------------------------------

## 0. VÌ SAO CHỌN ĐỀ TÀI NÀY (đọc 1 phút)

**Bài toán thật:** Mỗi lần có show/workshop/giải đấu/sự kiện CLB hot, hệ thống bán vé hoặc sập, hoặc bị bot hốt sạch, hoặc bán trùng ghế. Ban tổ chức nhỏ (CLB sinh viên, nhóm indie, workshop, giải esports/pickleball, concert nhỏ) không có hạ tầng chịu được "thundering herd" 5.000 người bấm mua cùng lúc 20:00:00.

**Vì sao hợp với mục tiêu của bạn:**

| Tiêu chí                            | Đánh giá                                                                                                                                             |
|-------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------|
| Có bài toán concurrency "thật"      | 5.000 người tranh 200 vé → phải đúng tuyệt đối (không oversell)                                                                                      |
| Microservices có lý do tách rõ ràng | Workload khác nhau hoàn toàn: waiting room (Redis, throughput cao) ≠ inventory (consistency cao) ≠ payment (external, webhook) ≠ catalog (đọc nhiều) |
| Event-driven tự nhiên               | Order → Payment → Ticket → Notification → Audit là chuỗi sự kiện thật, không phải "Kafka cho có"                                                     |
| Distributed transaction thật        | Checkout = Saga (reserve → pay → issue), có compensating action, có "thanh toán đến muộn sau khi hold hết hạn"                                       |
| Real-time thật                      | Vị trí trong hàng đợi, số vé còn lại, check-in live                                                                                                  |
| Nhiều role có trách nhiệm thật      | Attendee / Organizer / Event Staff / Moderator / Admin / System                                                                                      |
| Public được                         | Dùng thật cho CLB, workshop, sự kiện cộng đồng; có thể demo bằng "Flash sale simulation"                                                             |
| Khác biệt                           | Không phải e-commerce clone: lõi là **fairness + inventory contention + saga**, không phải giỏ hàng                                                  |

**Rủi ro thật (nói trước):**

- Thanh toán thật có pháp lý → **MVP chỉ dùng payment sandbox / mock provider tự dựng**, vé miễn phí hoặc "thanh toán giả lập" khi public.
- Dễ over-engineer vì có quá nhiều service → roadmap bên dưới **thêm service theo từng phase**, không dựng 10 service rồi mới chạy.
- Phần đáng giá nhất (inventory + saga + waiting room) khó test → bạn phải dành thời gian viết concurrency test và load test, không chỉ viết endpoint.

------------------------------------------------------------------------

## 1. PRODUCT

### 1.1 Problem

1.  **Oversell / bán trùng:** nhiều request cùng trừ tồn kho.
2.  **Sập hệ thống lúc mở bán:** traffic dồn cục.
3.  **Không công bằng:** bot, người có mạng nhanh, F5 liên tục thắng người thật.
4.  **Giữ chỗ vô thời hạn:** người ta giữ vé không thanh toán → vé "chết".
5.  **BTC nhỏ không có công cụ:** check-in bằng Excel, QR giả, vé bị chia sẻ nhiều lần.

### 1.2 Solution

- **Waiting room:** khi sự kiện mở bán, người dùng vào hàng đợi ảo; hệ thống thả vào từng đợt theo tốc độ mà phần mua vé chịu được.
- **Reservation có TTL:** chọn vé → giữ 10 phút → thanh toán → vé phát hành. Hết hạn tự nhả.
- **Saga checkout:** reserve → pay → issue, có bù trừ khi lỗi.
- **Vé có QR ký số (rotating / signed)**, check-in idempotent, chống quét 2 lần.
- **Dashboard cho Organizer:** doanh thu, tốc độ bán, check-in live.

### 1.3 Target users

| Nhóm                    | Ví dụ                                  | Nhu cầu                                    |
|-------------------------|----------------------------------------|--------------------------------------------|
| Người tham dự           | sinh viên, fan, người chơi             | mua vé công bằng, không sập, có vé QR      |
| Ban tổ chức nhỏ         | CLB, nhóm workshop, giải đấu cộng đồng | tạo sự kiện, bán vé, check-in, xem số liệu |
| Staff tại cổng          | tình nguyện viên                       | quét QR nhanh, không quét trùng            |
| Ban kiểm duyệt nền tảng | moderator                              | duyệt sự kiện, chống lừa đảo               |

### 1.4 User roles (mỗi role có trách nhiệm thật)

| Role                         | Trách nhiệm nghiệp vụ       | Quyền chính                                                                                                                       |
|------------------------------|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| **GUEST**                    | Xem sự kiện công khai       | Đọc catalog đã PUBLISHED, không mua được                                                                                          |
| **ATTENDEE**                 | Mua vé, quản lý vé của mình | Join waiting room, tạo order, xem/hủy order **của chính mình**, xem vé                                                            |
| **ORGANIZER**                | Sở hữu sự kiện              | CRUD sự kiện **do mình tạo**, cấu hình tier/giá/số lượng, submit duyệt, gán staff, xem báo cáo sự kiện của mình, yêu cầu hủy/hoàn |
| **EVENT_STAFF**              | Check-in tại cổng           | Chỉ check-in **sự kiện được gán**, chỉ xem trạng thái vé cần thiết (không xem thông tin thanh toán)                               |
| **MODERATOR**                | Kiểm duyệt nội dung         | Duyệt/từ chối sự kiện (kèm lý do), gỡ sự kiện vi phạm; **không** động vào tiền                                                    |
| **ADMIN**                    | Vận hành nền tảng           | Quản lý user/organizer verify, xem audit log, hoàn tiền thủ công, cấu hình rate limit/waiting room toàn cục                       |
| **SYSTEM** (service account) | Worker nội bộ               | Expire hold, reconcile order kẹt, replay DLQ — dùng machine token riêng, **không** dùng token của người                           |

**Nguyên tắc:** `ORGANIZER` không tự duyệt sự kiện của mình, `MODERATOR` không hoàn tiền, `STAFF` không đọc dữ liệu tiền. Đây là bài tập về **separation of duties**, không phải thêm role cho đẹp.

------------------------------------------------------------------------

## 2. FEATURES

### 2.1 MVP (bản chạy được end-to-end)

1.  Đăng ký/đăng nhập, JWT access + refresh token rotation.
2.  Organizer tạo sự kiện + ticket tier → submit → Moderator duyệt → PUBLISHED.
3.  Danh sách/tìm kiếm/lọc/phân trang sự kiện (Postgres full-text).
4.  Checkout: reserve (TTL 10p) → thanh toán (mock provider) → phát hành vé.
5.  Chống oversell tuyệt đối (test bằng concurrent test).
6.  Vé QR + check-in bởi staff, không check-in trùng.
7.  Notification (in-app + email qua outbox) khi order thành công/hết hạn.
8.  Idempotency-Key cho tạo order & thanh toán.
9.  Correlation ID xuyên suốt gateway → service → Kafka.

### 2.2 Advanced

- **Waiting room** (Redis ZSET + SSE, admission token có ký).
- **Rate limiting** ở gateway (per user / per IP / per event).
- **Refund & compensation** (hủy sự kiện → hoàn hàng loạt, xử lý async + retry).
- **Audit service** (append-only, consume mọi domain event).
- **Real-time dashboard** cho organizer (SSE: bán/giây, còn lại, check-in).
- **Bot mitigation nhẹ:** giới hạn số vé/người, proof-of-work/captcha giả lập ở bước vào phòng chờ.
- **Analytics read model** (CQRS-lite): bảng tổng hợp doanh thu theo giờ.
- **Waitlist** khi sold-out: có vé nhả (hold hết hạn/hủy) thì mời người kế tiếp.

------------------------------------------------------------------------

## 3. KIẾN TRÚC BACKEND

### 3.1 Tổng quan

``` text
                          ┌────────────────────────┐
                          │   Next.js (web)        │
                          │  SSR + CSR, SSE client │
                          └───────────┬────────────┘
                                      │ HTTPS
                          ┌───────────▼────────────┐
                          │      API Gateway       │
                          │ Spring Cloud Gateway   │
                          │ JWT verify · RateLimit │
                          │ CorrelationId · CORS   │
                          └───────────┬────────────┘
        ┌───────────┬─────────────┬───┴────────┬──────────────┬──────────────┐
        │           │             │            │              │              │
  ┌─────▼────┐ ┌────▼─────┐ ┌─────▼─────┐ ┌────▼─────┐ ┌──────▼─────┐ ┌──────▼──────┐
  │ identity │ │ catalog  │ │ waiting-  │ │  order   │ │   ticket   │ │notification │
  │          │ │          │ │  room     │ │ (saga)   │ │ (+checkin) │ │ (+SSE push) │
  └─────┬────┘ └────┬─────┘ └─────┬─────┘ └──┬────┬──┘ └──────┬─────┘ └──────┬──────┘
        │           │             │          │    │gRPC       │              │
     PG:identity PG:catalog    Redis      PG:order │      PG:ticket       PG:notif
                                              ┌────▼─────┐  ┌──────────┐
                                              │inventory │  │ payment  │──► Mock Payment
                                              │          │  │          │    Provider (webhook)
                                              └────┬─────┘  └────┬─────┘
                                            PG:inventory     PG:payment
                                            + Redis(cache)

   ═══════════════════════════ Kafka (event backbone) ════════════════════════════
     catalog.events · inventory.events · order.events · payment.events · ticket.events
                                              │
                                      ┌───────▼───────┐
                                      │ audit-service │ ──► PG:audit (append-only)
                                      └───────────────┘

   Observability: OpenTelemetry → Jaeger · Prometheus → Grafana · Loki (optional)
```

### 3.2 Ranh giới từng service & vì sao tách

| Service                  | Sở hữu                                                   | Vì sao **phải** tách                                                                                 | Công nghệ dữ liệu                                         |
|--------------------------|----------------------------------------------------------|------------------------------------------------------------------------------------------------------|-----------------------------------------------------------|
| **api-gateway**          | routing, authN thô, rate limit, correlation id           | Điểm vào duy nhất; chặn traffic xấu trước khi chạm service                                           | Redis (rate limit)                                        |
| **identity-service**     | user, credential, role, refresh token, organizer profile | Bảo mật là bounded context riêng; blast radius khi lộ lỗi lớn                                        | PostgreSQL                                                |
| **catalog-service**      | event, tier, review workflow, staff assignment, search   | Read-heavy, đổi chậm, cần cache/search riêng, có workflow duyệt                                      | PostgreSQL                                                |
| **waiting-room-service** | hàng đợi, admission token                                | Workload hoàn toàn khác: nhiều kết nối SSE, ghi/đọc Redis tốc độ cao, scale ngang độc lập lúc mở bán | Redis (+ Postgres nhỏ nếu cần)                            |
| **inventory-service**    | tồn kho, hold (giữ chỗ), TTL                             | **Nơi duy nhất quyết định "còn vé hay không"**. Cần consistency mạnh, contention cao                 | PostgreSQL (source of truth) + Redis (tăng tốc, tùy chọn) |
| **order-service**        | order, saga orchestrator, order state machine            | Điều phối checkout; là "nhạc trưởng"                                                                 | PostgreSQL                                                |
| **payment-service**      | payment, webhook, refund                                 | Ranh giới với hệ thống ngoài; bắt buộc idempotent; cô lập dữ liệu nhạy cảm                           | PostgreSQL                                                |
| **ticket-service**       | ticket, QR, check-in                                     | Check-in có lưu lượng/đặc tính riêng (burst tại cổng, cần chạy được khi các service khác chậm)       | PostgreSQL                                                |
| **notification-service** | thông báo, email, SSE push                               | Thuần consumer; lỗi gửi mail **không được** làm hỏng checkout                                        | PostgreSQL + Redis (pub/sub)                              |
| **audit-service**        | audit log                                                | Append-only, consume tất cả; tách để không ai sửa được log                                           | PostgreSQL                                                |

**Cố ý KHÔNG tách:**

- `pricing`, `promotion`, `venue`, `search` → nằm trong **catalog** (chưa đủ lý do tách).
- `user-profile` → nằm trong **identity**.
- `analytics` → ban đầu là consumer nhỏ trong **audit/notification**; chỉ tách khi có workload riêng (v3).

### 3.3 Ai được gọi ai

``` text
Gateway ──► mọi service (REST)
order ──gRPC──► inventory         (reserve/release: sync, cần trả lời ngay cho user)
order ──REST──► catalog           (lấy giá/tier snapshot lúc tạo order; có cache + timeout + circuit breaker)
payment ◄──webhook── Mock Provider
Mọi thứ còn lại: Kafka events (async)
```

**Quy tắc cứng:**

1.  Service **không** đọc DB của service khác. Cần dữ liệu → gọi API hoặc giữ **local read model** cập nhật qua event.
2.  Không có vòng gọi sync: `catalog → order → catalog` là cấm.
3.  Ghi vào 2 nơi (DB + Kafka) **bắt buộc** qua Outbox.
4.  Consumer **bắt buộc** idempotent (bảng `processed_events`).

### 3.4 Sync vs Async — quyết định cụ thể

| Tình huống                             | Chọn                            | Lý do                                                                                                                                                        |
|----------------------------------------|---------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Giữ vé khi bấm "Mua"                   | **gRPC sync** (order→inventory) | User cần biết ngay "giữ được hay không"; latency thấp, hợp đồng chặt. *Bắt đầu bằng REST, đo, rồi đổi sang gRPC để học — đây là USEFUL, không phải bắt buộc* |
| Lấy giá tier lúc tạo order             | REST sync + cache               | Đọc đơn giản, chấp nhận stale ngắn vì có snapshot giá trong order                                                                                            |
| Thanh toán thành công → cập nhật order | **Kafka event**                 | Webhook đến lúc nào không biết, order có thể đang down                                                                                                       |
| Order paid → phát hành vé              | **Kafka event**                 | Không bắt user chờ; retry được                                                                                                                               |
| Gửi email/notification                 | **Kafka event**                 | Không được ảnh hưởng luồng chính                                                                                                                             |
| Audit log                              | **Kafka event**                 | Fire-and-forget, nhiều consumer                                                                                                                              |
| Số vé còn lại cho UI                   | SSE/poll từ read model          | Chấp nhận eventual consistency; **quyết định thật vẫn ở inventory**                                                                                          |

### 3.5 Có cần không?

| Thành phần                        | Quyết định                           | Lý do                                                                                                          |
|-----------------------------------|--------------------------------------|----------------------------------------------------------------------------------------------------------------|
| API Gateway                       | **CÓ**                               | Rate limiting, JWT, correlation id là bài học thật                                                             |
| Service Discovery (Eureka/Consul) | **KHÔNG**                            | Docker Compose DNS đủ; deploy VPS không cần. *OVERENGINEERING*                                                 |
| Config Server                     | **KHÔNG**                            | Env vars + Docker secrets + Spring profiles                                                                    |
| Kafka                             | **CÓ**                               | Có ≥5 luồng event thật, cần replay/ordering/DLQ                                                                |
| Redis                             | **CÓ, nhưng có điểm dừng**           | Waiting room (bắt buộc), rate limit, cache catalog. **Không** dùng Redis làm source of truth tồn kho ở bản đầu |
| gRPC                              | **1 chỗ duy nhất** (order→inventory) | Đủ để học; thêm nữa là thừa                                                                                    |
| Elasticsearch                     | **KHÔNG**                            | Postgres FTS + `pg_trgm` đủ; *OVERENGINEERING*                                                                 |
| Kubernetes                        | **KHÔNG**                            | Docker Compose trên VPS                                                                                        |
| Service Mesh                      | **KHÔNG**                            |                                                                                                                |

### 3.6 Cấu trúc code trong mỗi service (Hexagonal-nhẹ)

``` text
service/
 ├─ api/            # controller, request/response DTO, exception handler
 ├─ application/    # use case / service, transaction boundary
 ├─ domain/         # entity, value object, state machine, domain event, rule thuần
 ├─ infrastructure/ # JPA repo, Kafka producer/consumer, gRPC/REST client, Redis
 └─ config/
```

Domain **không import** Spring/JPA annotation nặng nề nếu có thể; nhưng đừng tách `domain model` ≠ `JPA entity` ngay từ đầu — đó là abstraction quá sớm. Chỉ tách khi thấy đau.

### 3.7 Monorepo

``` text
qflow/
 ├─ services/{gateway,identity,catalog,waiting-room,inventory,order,payment,ticket,notification,audit}
 ├─ libs/
 │   ├─ event-contracts/   # CHỈ schema event + envelope (versioned)
 │   └─ common-web/        # error model, correlation filter (mỏng thôi!)
 ├─ web/                   # Next.js
 ├─ tools/mock-payment/    # mock provider: delay, fail, duplicate webhook
 ├─ tools/load/            # k6 scripts
 ├─ infra/{compose,grafana,prometheus,caddy}
 └─ docs/
```

> ⚠️ `libs/` phình to = distributed monolith. Quy tắc: **không chứa logic nghiệp vụ**.

------------------------------------------------------------------------

## 4. BUSINESS LOGIC & STATE MACHINE

### 4.1 Event (catalog)

``` text
DRAFT ──submit──► PENDING_REVIEW ──approve(MODERATOR)──► PUBLISHED ──sale_starts_at──► ON_SALE
                       │                                       │                          │
                    reject                                  cancel                  sold out / sale_ends
                       ▼                                       ▼                          ▼
                    REJECTED ──edit&resubmit──► DRAFT      CANCELLED               SALE_CLOSED ──► ENDED
```

| Chuyển state                      | Ai                      | Điều kiện                                                                 |
|-----------------------------------|-------------------------|---------------------------------------------------------------------------|
| DRAFT→PENDING_REVIEW              | ORGANIZER (chủ sự kiện) | Đủ field, ≥1 tier, giờ hợp lệ, organizer đã verified                      |
| PENDING_REVIEW→PUBLISHED/REJECTED | MODERATOR               | **Không phải người tạo**; reject bắt buộc có lý do                        |
| PUBLISHED→ON_SALE                 | SYSTEM (scheduler)      | Đến `sale_starts_at`                                                      |
| \*→CANCELLED                      | ORGANIZER / ADMIN       | Nếu đã có vé bán → phát event `EventCancelled` kích hoạt refund hàng loạt |

**Câu hỏi tự nghĩ:** Organizer sửa giá tier khi sự kiện đang ON_SALE thì sao? (gợi ý: order cũ giữ **price snapshot**; đổi `total_quantity` xuống thấp hơn `sold` thì sao?)

### 4.2 Order — state machine (orchestrated saga)

``` text
                       reserve OK                    PaymentSucceeded
   PENDING ──────────► AWAITING_PAYMENT ───────────────────► PAID ──TicketsIssued──► COMPLETED
      │ reserve FAIL          │  │                              │
      ▼                       │  └─ user cancel ─► CANCELLED    └─ issue FAIL (retry hết) ─► REFUND_PENDING ─► REFUNDED
   REJECTED                   │
                         hold expired
                              ▼
                           EXPIRED ◄──(PaymentSucceeded đến muộn) ──► REFUND_PENDING ──► REFUNDED
```

### 4.3 Hold (inventory)

``` text
ACTIVE ──order PAID──► CONSUMED
   │
   ├─ user cancel / order failed ─► RELEASED
   └─ now > expires_at (worker) ──► EXPIRED
```

### 4.4 Luồng checkout đầy đủ

``` text
Attendee ─► Gateway ─► order: POST /orders (Idempotency-Key, admission token nếu có waiting room)
   1. order: tx1 {insert order PENDING}                          [commit]
   2. order ─gRPC─► inventory.Reserve(orderId, items, ttl=10m)   [idempotent theo orderId]
        inventory: tx { UPDATE stock ... WHERE available >= qty ; INSERT hold ; outbox }
   3. order: tx2 {order=AWAITING_PAYMENT, outbox:OrderAwaitingPayment}
   4. Client ─► payment: POST /payments (Idempotency-Key) → redirect Mock Provider
   5. Provider ─webhook─► payment: verify signature → dedupe(provider_event_id) → tx{payment=SUCCEEDED, outbox:PaymentSucceeded}
   6. order consume PaymentSucceeded: tx{order=PAID, outbox:OrderPaid}
   7. inventory consume OrderPaid: hold CONSUMED, reserved→sold
      ticket consume OrderPaid: issue tickets, outbox:TicketsIssued
   8. order consume TicketsIssued: COMPLETED
   9. notification + audit consume mọi event
```

**Các điểm "đau" bạn phải tự giải (đây là phần học chính):**

1.  Bước 2 → 3: order crash **sau khi** inventory reserve thành công. → ai dọn? (reconciler / expiry)
2.  Bước 5: webhook gửi 3 lần. → dedupe thế nào, trả HTTP code gì cho provider?
3.  Webhook `PaymentSucceeded` đến **sau khi** order EXPIRED. → hoàn tiền tự động (compensation), hay cứu order nếu còn vé?
4.  User bấm "Pay" 5 lần. → Idempotency-Key + unique constraint.
5.  Kafka down lúc bước 5. → outbox giữ event, relay gửi sau.
6.  2 `OrderPaid` event được inventory xử lý 2 lần. → `processed_events`.
7.  Ticket-service down 30 phút. → order kẹt PAID; retry + DLQ + alert.

### 4.5 Race conditions chính

| Race                                         | Giải pháp (bạn tự chọn, rồi so sánh)                                                                                                             |
|----------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| 2 request cùng trừ 1 vé cuối                 | \(a\) `UPDATE … WHERE available >= :q` atomic **(chọn trước)**, (b) optimistic lock + retry, (c) `SELECT FOR UPDATE`, (d) Redis Lua. Đo từng cái |
| Cùng user mua vượt `max_per_user` bằng 2 tab | unique/atomic counter theo `(user, tier)` trong cùng tx                                                                                          |
| Expire worker chạy cùng lúc 2 instance       | `SELECT … FOR UPDATE SKIP LOCKED` hoặc ShedLock                                                                                                  |
| User cancel đúng lúc payment thành công      | version (optimistic locking) trên order + state machine từ chối transition sai                                                                   |

------------------------------------------------------------------------

## 5. DATABASE DESIGN (mỗi service 1 DB riêng)

> Mỗi service có **schema/DB riêng**, user DB riêng. Không có cross-service FK — chỉ lưu ID (uuid) tham chiếu. Lý do: nếu cho phép service A đọc bảng của B thì schema của B bị đóng băng (không ai dám đổi), mất khả năng deploy độc lập, và bạn có "distributed monolith". Dùng **Flyway** cho mọi migration.

Quy ước chung: `id uuid` (UUIDv7 nếu thư viện hỗ trợ, tốt cho index locality), `created_at`, `updated_at`, `version bigint` (optimistic lock) ở bảng có cạnh tranh.

### 5.1 identity-db

``` text
users(id PK, email UNIQUE(lower), password_hash, status[ACTIVE|LOCKED|DELETED], created_at, updated_at, deleted_at)
user_roles(user_id FK, role, PRIMARY KEY(user_id, role))
organizer_profiles(user_id PK/FK, org_name, status[PENDING|VERIFIED|REJECTED], verified_by, verified_at)
refresh_tokens(id PK, user_id FK, token_hash UNIQUE, family_id, expires_at, revoked_at, replaced_by, user_agent, ip, created_at)
  INDEX(user_id), INDEX(family_id)
```

**Vì sao:** lưu **hash** refresh token (lộ DB không lộ token); `family_id` để phát hiện *token reuse* (token cũ bị dùng lại → thu hồi cả family). Email unique trên `lower(email)` tránh trùng hoa/thường. Soft delete cho user vì audit/order cần tham chiếu.

### 5.2 catalog-db

``` text
events(id PK, organizer_id, title, slug UNIQUE, description, venue, city, starts_at, ends_at,
       sale_starts_at, sale_ends_at, status, search_vector tsvector, version, created_at, updated_at, deleted_at)
  INDEX(status, starts_at)               -- list công khai
  INDEX(organizer_id, status)            -- dashboard organizer
  GIN(search_vector)                     -- full-text
ticket_tiers(id PK, event_id FK, name, price_amount bigint, currency, total_quantity, max_per_order, max_per_user, sort_order, version)
  UNIQUE(event_id, name)
event_reviews(id PK, event_id FK, reviewer_id, decision, reason, created_at)
event_status_history(id PK, event_id FK, from_status, to_status, actor_id, actor_role, created_at)
event_staff(event_id FK, staff_id, assigned_by, PRIMARY KEY(event_id, staff_id))
outbox_events(...)
```

**Vì sao:** tiền lưu `bigint` đơn vị nhỏ nhất (không dùng float). `status_history` để audit/tranh chấp. `price` ở tier chỉ là **giá hiện tại**; order lưu snapshot riêng.

### 5.3 inventory-db *(quan trọng nhất)*

``` text
tier_stock(tier_id PK, event_id, total, reserved, sold, version,
           CHECK(reserved >= 0 AND sold >= 0 AND reserved + sold <= total))
holds(id PK, order_id, tier_id FK, user_id, quantity, status[ACTIVE|CONSUMED|RELEASED|EXPIRED], expires_at, created_at)
  UNIQUE(order_id, tier_id)                               -- idempotent reserve
  INDEX(expires_at) WHERE status='ACTIVE'                 -- partial index cho expiry worker
user_tier_purchases(user_id, tier_id, quantity, PRIMARY KEY(user_id, tier_id))
outbox_events, processed_events(event_id PK, consumer, processed_at)
```

**Vì sao:** `CHECK` constraint là **lớp phòng thủ cuối** — kể cả code sai, DB vẫn từ chối oversell. `UNIQUE(order_id, tier_id)` làm reserve idempotent. Partial index giữ index nhỏ vì phần lớn hold đã kết thúc.

Reserve (ý tưởng, **bạn tự viết**): một câu `UPDATE tier_stock SET reserved = reserved + :q WHERE tier_id = :id AND total - reserved - sold >= :q` rồi kiểm tra số dòng bị ảnh hưởng.

### 5.4 order-db

``` text
orders(id PK, user_id, event_id, status, total_amount, currency, idempotency_key, expires_at, version, created_at, updated_at)
  UNIQUE(user_id, idempotency_key)
  INDEX(user_id, created_at DESC), INDEX(status, expires_at)   -- reconciler tìm order kẹt
order_items(id PK, order_id FK, tier_id, tier_name, unit_price, quantity)   -- price/name SNAPSHOT
order_status_history(...)
outbox_events, processed_events
```

### 5.5 payment-db

``` text
payments(id PK, order_id UNIQUE-per-active, amount, currency, status[CREATED|PENDING|SUCCEEDED|FAILED|REFUNDED], provider, provider_txn_id UNIQUE, idempotency_key UNIQUE, version)
webhook_events(provider_event_id PK, payload jsonb, signature_valid, received_at, processed_at)
refunds(id PK, payment_id FK, amount, status, reason, idempotency_key UNIQUE)
outbox_events, processed_events
```

**Vì sao:** `webhook_events.provider_event_id` PK = chống xử lý webhook trùng ở tầng DB. Lưu raw payload để debug tranh chấp.

### 5.6 ticket-db

``` text
tickets(id PK, order_id, event_id, tier_id, owner_id, code UNIQUE, status[ISSUED|CHECKED_IN|VOID], issued_at, checked_in_at, checked_in_by, version)
  UNIQUE(order_id, tier_id, seq)    -- idempotent issuance
  INDEX(event_id, status)
check_in_logs(id PK, ticket_id, staff_id, result[OK|DUPLICATE|INVALID|WRONG_EVENT], scanned_at)
event_staff_replica(event_id, staff_id)          -- LOCAL READ MODEL từ catalog events
event_replica(event_id PK, starts_at, ends_at, status)
```

**Vì sao:** `event_staff_replica` — ticket-service phải biết staff nào được check-in sự kiện nào mà **không gọi catalog lúc quét** (cổng đông, catalog chậm không được làm nghẽn). Đây là bài học **data replication qua event + eventual consistency**.

### 5.7 notification-db / audit-db

``` text
notifications(id PK, user_id, type, title, body, payload jsonb, read_at, created_at)  INDEX(user_id, created_at DESC)
deliveries(id PK, notification_id, channel, status, attempts, last_error, next_retry_at)  UNIQUE(source_event_id, channel)
audit_logs(id PK, occurred_at, actor_id, actor_role, action, resource_type, resource_id, correlation_id, payload jsonb)
  -- append-only: REVOKE UPDATE/DELETE, partition theo tháng (ADVANCED)
```

### 5.8 Redis dùng cho gì (và không dùng cho gì)

| Key                                          | Dùng cho                     | Service                                    |
|----------------------------------------------|------------------------------|--------------------------------------------|
| `wr:{eventId}:queue` (ZSET, score=join time) | hàng đợi                     | waiting-room                               |
| `wr:{eventId}:admitted:{userId}` (TTL)       | đã được vào                  | waiting-room                               |
| `rl:{route}:{userId/ip}`                     | rate limit                   | gateway                                    |
| `cache:event:{id}`                           | cache catalog                | catalog                                    |
| `avail:{tierId}`                             | **cache hiển thị** số vé còn | inventory (chỉ hiển thị, không quyết định) |

**Không** dùng Redis làm source of truth tồn kho ở v1: Redis chết/mất data = mất tiền. Ta học Redis Lua ở phase tối ưu, **sau khi đo** Postgres là bottleneck.

------------------------------------------------------------------------

## 6. MESSAGE / EVENT ARCHITECTURE

### 6.1 Topics (key = aggregate id để giữ thứ tự theo aggregate)

| Topic                 | Key     | Events                                                                                                       | Producer  | Consumers                                            |
|-----------------------|---------|--------------------------------------------------------------------------------------------------------------|-----------|------------------------------------------------------|
| `catalog.events.v1`   | eventId | EventPublished, EventCancelled, TierChanged, StaffAssigned                                                   | catalog   | inventory, ticket, notification, audit, waiting-room |
| `inventory.events.v1` | tierId  | StockReserved, ReservationRejected, HoldExpired, StockCommitted                                              | inventory | order, notification, audit                           |
| `order.events.v1`     | orderId | OrderCreated, OrderAwaitingPayment, OrderPaid, OrderCompleted, OrderExpired, OrderCancelled, RefundRequested | order     | payment, inventory, ticket, notification, audit      |
| `payment.events.v1`   | orderId | PaymentSucceeded, PaymentFailed, RefundSucceeded                                                             | payment   | order, notification, audit                           |
| `ticket.events.v1`    | orderId | TicketsIssued, TicketCheckedIn                                                                               | ticket    | order, notification, audit                           |
| `*.dlq`               | —       | message lỗi sau N retry                                                                                      | consumer  | ADMIN/SYSTEM replay                                  |

### 6.2 Envelope

``` json
{
  "eventId": "uuid",            // dedupe
  "type": "OrderPaid",
  "version": 1,                 // schema version
  "occurredAt": "...",
  "aggregateId": "orderId",
  "correlationId": "…",         // trace xuyên hệ thống
  "causationId": "…",           // event/command gây ra nó
  "payload": { }
}
```

### 6.3 Bắt buộc

- **Transactional Outbox** ở mọi producer: ghi `outbox_events` cùng transaction với dữ liệu; relay (poller `FOR UPDATE SKIP LOCKED`) đẩy lên Kafka. *Đừng* vội dùng Debezium — poller đủ để hiểu bản chất.
- **Inbox/processed_events** ở mọi consumer: `INSERT … ON CONFLICT DO NOTHING` + xử lý trong cùng tx.
- **Retry có backoff** (retry topic) → **DLQ**. Phân biệt lỗi *tạm thời* (retry) và lỗi *vĩnh viễn* (DLQ ngay, ví dụ payload sai schema).
- **Delivery = at-least-once**, đừng tin "exactly-once". Idempotent consumer mới là đáp án.
- Event schema **backward compatible** (chỉ thêm field optional).

### 6.4 Saga: orchestration hay choreography?

- **Checkout = orchestration** (order-service là orchestrator, có state machine rõ ràng) → dễ debug, dễ thấy order đang kẹt ở đâu.
- **Notification/Audit/Analytics = choreography** (chỉ nghe event, không điều khiển ai).
- **Vì sao không 2PC/XA:** blocking, coupling, Kafka và payment provider không tham gia XA. Ta chấp nhận eventual consistency + compensating action.

------------------------------------------------------------------------

## 7. SECURITY ARCHITECTURE

``` text
Login ─► identity: verify password (Argon2id/BCrypt)
      ─► Access Token  (JWT RS256, 10–15 phút, claims: sub, roles, jti)  → memory (Next.js)
      ─► Refresh Token (opaque random 256-bit, 7–14 ngày)               → httpOnly + Secure + SameSite=Strict cookie, path=/auth/refresh
Request ─► Gateway: verify chữ ký + exp + rate limit + coarse role check (route-level)
        ─► Service: verify lại JWT (JWKS từ identity) + FINE-GRAINED check (ownership/ABAC)
```

| Câu hỏi                     | Quyết định                                                                                                                                                                                                                                 |
|-----------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Token lưu đâu?              | Access trong memory (không localStorage → giảm XSS exfiltration); Refresh trong httpOnly cookie                                                                                                                                            |
| Refresh xử lý?              | **Rotation**: mỗi lần refresh cấp token mới, token cũ revoke. Token cũ bị dùng lại → **reuse detection** → revoke cả `family_id`, buộc login lại                                                                                           |
| Logout?                     | Revoke refresh token (+ family). Access token chết tự nhiên sau ≤15p; cần chặn gấp thì `jti` denylist trong Redis (OPTIONAL)                                                                                                               |
| CSRF?                       | Cookie chỉ dùng cho `/auth/refresh`; SameSite=Strict + kiểm tra Origin/custom header                                                                                                                                                       |
| Password?                   | Argon2id (hoặc BCrypt cost ≥ 12), rate limit login, lock tạm sau N lần sai                                                                                                                                                                 |
| Gateway check gì?           | Có token hợp lệ không, route cần role gì (coarse), rate limit                                                                                                                                                                              |
| Service check gì?           | **Luôn** tự check: user có sở hữu resource này không? (ví dụ ORGANIZER chỉ sửa event của mình; STAFF chỉ check-in event được gán)                                                                                                          |
| Chống privilege escalation? | Không bao giờ tin `userId`/`role` trong body/header do client gửi; lấy từ JWT; đổi role chỉ ADMIN và ghi audit; JWT chứa roles nhưng thao tác nhạy cảm có thể kiểm lại DB; header nội bộ (`X-User-Id`) **bị gateway xóa** nếu từ ngoài vào |
| Service-to-service?         | Machine token (client credentials) cho SYSTEM; mạng nội bộ Docker không expose port service ra ngoài, chỉ gateway public                                                                                                                   |
| Secret?                     | Env/Docker secrets, không commit; `.env.example`                                                                                                                                                                                           |
| Webhook?                    | HMAC signature + timestamp tolerance + dedupe event id                                                                                                                                                                                     |
| QR vé?                      | Chứa `ticketId` + chữ ký (HMAC/Ed25519) — không đoán/giả được; server vẫn là nguồn sự thật                                                                                                                                                 |

------------------------------------------------------------------------

## 8. FRONTEND (Next.js + React + TypeScript)

Mục tiêu: **UI vừa đủ, giao tiếp Backend như hệ thống thật**. Không tốn thời gian animation.

### 8.1 Page structure

``` text
/                       Landing + sự kiện nổi bật (SSR/ISR)
/events                 Danh sách: search, filter (city/date/price), pagination (cursor)
/events/[slug]          Chi tiết + tier + nút Mua / Vào phòng chờ
/queue/[eventId]        Phòng chờ: vị trí, ETA, tự chuyển khi được admit (SSE)
/checkout/[orderId]     Countdown hold 10p, thanh toán, trạng thái order (SSE/poll)
/me/orders, /me/tickets Vé QR
/auth/{login,register}
/organizer/events       Danh sách, tạo/sửa, submit duyệt
/organizer/events/[id]/{tiers,staff,dashboard}   Dashboard real-time
/staff/checkin/[eventId]  Quét QR (camera), lịch sử quét
/moderator/queue        Hàng chờ duyệt
/admin/{users,audit,refunds,dlq}
```

### 8.2 Kỹ thuật cần thể hiện

| Chủ đề        | Gợi ý                                                                                                                              |
|---------------|------------------------------------------------------------------------------------------------------------------------------------|
| API client    | 1 wrapper typed (fetch + zod), tự gắn token, tự refresh khi 401 (single-flight để tránh refresh song song), gắn `X-Correlation-Id` |
| Auth          | Access token trong memory; middleware Next.js chặn route theo role; không hardcode role check chỉ ở UI (UI chỉ để UX)              |
| State         | TanStack Query cho server state; Zustand/Context cho UI state nhỏ. **Không** Redux                                                 |
| Form          | react-hook-form + zod, hiển thị lỗi field từ backend (`errors[]` trong error response)                                             |
| Error/Loading | Error boundary, skeleton, retry; phân biệt 4xx (user sửa) / 5xx (thử lại) / 429 (chờ)                                              |
| Idempotency   | Sinh `Idempotency-Key` **một lần** cho mỗi "ý định mua", dùng lại khi retry; disable nút + optimistic UI                           |
| Real-time     | SSE cho queue position/order status/dashboard; tự reconnect với `Last-Event-ID`                                                    |
| Optimistic UI | Đánh dấu thông báo đã đọc, nhưng **không** optimistic cho inventory (UI chỉ hiển thị "đang giữ vé…")                               |
| Responsive    | Tailwind; mobile-first (staff check-in dùng điện thoại)                                                                            |

------------------------------------------------------------------------

## 9. OBSERVABILITY

``` text
Request ─► Gateway sinh X-Correlation-Id (nếu chưa có)
        ─► propagate qua HTTP header / gRPC metadata / Kafka header
        ─► MỌI log line có: timestamp, level, service, correlationId, traceId, userId(hash), eventId, orderId
```

| Hạng mục                  | Công cụ                                                                                               | Trả lời câu hỏi                                          |
|---------------------------|-------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| Structured logging (JSON) | Logback JSON + Loki (hoặc \`docker logs                                                               | jq\` lúc đầu)                                            |
| Distributed tracing       | OpenTelemetry → Jaeger/Tempo                                                                          | "Request đi qua service nào? Chậm ở đâu?"                |
| Metrics                   | Micrometer → Prometheus → Grafana                                                                     | RPS, latency p50/p95/p99, error rate, HikariCP pool, JVM |
| Kafka                     | consumer lag (kafka-exporter), DLQ size, outbox backlog (`count(*) where published_at is null`)       | "Event đang delay ở đâu?"                                |
| DB                        | `pg_stat_statements`, slow query log                                                                  | "Query nào chậm?"                                        |
| Health                    | Actuator `liveness`/`readiness` (readiness check DB/Kafka)                                            |                                                          |
| Business metrics          | `orders_created`, `holds_expired`, `oversell_attempts_rejected`, `queue_length`, `checkin_duplicates` |                                                          |

**Dashboard tối thiểu:** (1) Golden signals từng service, (2) Saga funnel: created → reserved → paid → completed → expired, (3) Kafka lag + DLQ, (4) Queue length & admit rate.

> Để Compose nhẹ: bắt đầu **Prometheus + Grafana + Jaeger**. Loki thêm sau.

------------------------------------------------------------------------

## 10. REAL-WORLD FAILURE SCENARIOS

Format: **Problem → Why → Detect → Prevent → Recover → Trade-off**

| \#  | Problem                                          | Why                                         | Detect                                     | Prevent                                                                                                              | Recover                                                                   | Trade-off                                                  |
|-----|--------------------------------------------------|---------------------------------------------|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------|------------------------------------------------------------|
| 1   | **Postgres inventory down**                      | crash/hết connection                        | readiness fail, Hikari pending↑            | pool size hợp lý, timeout ngắn, circuit breaker ở order                                                              | order trả 503 + `Retry-After`; không tạo order nửa vời                    | chọn **fail fast** thay vì cho đặt "tạm" (mất consistency) |
| 2   | **Redis down**                                   | OOM/network                                 | health, error rate                         | waiting-room: **fail-closed hay fail-open?** (tự quyết). Rate limit: fail-open có giới hạn local. Cache: fallback DB | khởi động lại, rebuild queue từ…? (nghĩ xem)                              | availability vs fairness                                   |
| 3   | **Kafka down**                                   | broker chết                                 | producer error, outbox backlog↑            | **Outbox** → ghi DB trước                                                                                            | relay tự gửi bù khi Kafka sống lại                                        | checkout vẫn chạy nhưng hoàn tất chậm (eventual)           |
| 4   | **Network timeout order→inventory**              | mạng/GC pause                               | timeout metric                             | timeout + retry với **cùng orderId** (idempotent)                                                                    | gọi lại Reserve; kết quả "trạng thái không biết" → query hold by orderId  | retry có thể gây tải thêm (retry storm) → backoff + jitter |
| 5   | **Order crash sau reserve, trước update**        | process kill                                | order kẹt PENDING quá N giây               | hold có TTL                                                                                                          | **Reconciler**: quét PENDING cũ → hỏi inventory → sửa trạng thái / hủy    | thêm 1 job nền phải vận hành                               |
| 6   | **Duplicate request (double click, retry mạng)** | client/proxy retry                          | unique violation `(user, idempotency_key)` | Idempotency-Key + unique constraint                                                                                  | trả lại **kết quả cũ** (cùng body, 200/201)                               | lưu key thêm storage, cần TTL cleanup                      |
| 7   | **Duplicate event / xử lý 2 lần**                | at-least-once, rebalance                    | `processed_events` conflict                | idempotent consumer                                                                                                  | bỏ qua                                                                    | thêm 1 write mỗi event                                     |
| 8   | **Concurrent update cùng resource**              | 2 tx đọc cũ ghi đè                          | `OptimisticLockException`                  | `@Version`, atomic UPDATE                                                                                            | 409 + client refetch / retry có giới hạn                                  | retry nhiều khi contention cao                             |
| 9   | **Webhook thanh toán đến sau khi hold hết hạn**  | mạng chậm, user trả muộn                    | PaymentSucceeded cho order EXPIRED         | state machine từ chối                                                                                                | **Hoàn tiền tự động** hoặc cứu order nếu stock còn (quyết định + ghi ADR) | UX vs độ phức tạp                                          |
| 10  | **Payment webhook giả mạo**                      | attacker gọi endpoint                       | signature invalid                          | HMAC + timestamp + IP allowlist (optional)                                                                           | log + alert                                                               | thêm chi phí verify                                        |
| 11  | **Ticket-service down sau khi đã thu tiền**      | crash/deploy                                | order kẹt PAID, consumer lag               | retry vô hạn có backoff, DLQ, alert theo tuổi order                                                                  | replay; nếu vĩnh viễn → refund                                            | thời gian user chờ vé                                      |
| 12  | **Partial failure phát hành vé (3/5 vé)**        | lỗi giữa vòng lặp                           | số ticket ≠ quantity                       | phát hành trong **1 transaction** + `UNIQUE(order_id,tier_id,seq)`                                                   | chạy lại idempotent                                                       | tx dài hơn                                                 |
| 13  | **Consumer chậm → lag**                          | xử lý nặng, DB chậm                         | lag metric                                 | tăng partition/consumer, batch, tối ưu query                                                                         | scale consumer                                                            | thứ tự theo key vẫn phải giữ                               |
| 14  | **Poison message**                               | payload hỏng                                | lỗi lặp cùng offset                        | validate schema, DLQ sau N lần                                                                                       | sửa → replay từ DLQ                                                       | cần tooling replay                                         |
| 15  | **Service restart giữa lúc xử lý Kafka message** | deploy                                      | message được giao lại                      | commit offset **sau** khi tx DB commit                                                                               | idempotent consumer                                                       | —                                                          |
| 16  | **Retry storm**                                  | mọi client retry đồng loạt khi service chậm | tăng RPS đột ngột + 5xx                    | exponential backoff + jitter, circuit breaker, `429 Retry-After`                                                     | bulkhead, shed load                                                       | từ chối bớt request để cứu hệ thống                        |
| 17  | **Clock skew/TTL**                               | nhiều node lệch giờ                         | hold hết hạn sai                           | dùng **thời gian DB** (`now()`) cho expiry                                                                           | —                                                                         | —                                                          |
| 18  | **Cache inconsistency** (giá/availability cũ)    | invalidate trễ                              | so sánh cache vs DB                        | TTL ngắn, invalidate theo event, **quyết định ghi luôn ở DB**                                                        | xóa cache                                                                 | stale hiển thị chấp nhận được, stale quyết định thì không  |
| 19  | **Bot/thundering herd**                          | script spam                                 | rate limit hit, queue join bất thường      | waiting room + rate limit + giới hạn/người                                                                           | block, tăng PoW                                                           | friction cho người thật                                    |

------------------------------------------------------------------------

## 11. TESTING STRATEGY

| Tầng                      | Test cái gì                                                                                                     | Mock ở đâu                                | Công cụ                                                                               |
|---------------------------|-----------------------------------------------------------------------------------------------------------------|-------------------------------------------|---------------------------------------------------------------------------------------|
| **Unit**                  | State machine, rule `max_per_user`, tính tiền, logic tạo/verify QR, mapper                                      | Mock repo/client                          | JUnit5, Mockito, AssertJ                                                              |
| **Integration (service)** | Repository + Flyway migration, query phức tạp, outbox relay, consumer idempotent, controller + security         | **Không mock DB/Kafka/Redis** — dùng thật | **Testcontainers** (Postgres, Kafka, Redis), `@SpringBootTest`, MockMvc/WebTestClient |
| **Concurrency**           | 500 thread mua 100 vé → đúng 100 thành công, 0 oversell; 2 worker expire cùng lúc; webhook gửi 10 lần song song | không mock                                | `ExecutorService` + `CountDownLatch`, Testcontainers                                  |
| **Contract**              | event schema, gRPC proto                                                                                        | —                                         | Schema test trong `event-contracts`; Pact (OPTIONAL)                                  |
| **E2E**                   | `Next.js → Gateway → order → inventory → payment(mock) → ticket → notification` happy path + 3 failure path     | provider = mock-payment tool              | Playwright + Docker Compose test profile                                              |
| **Load**                  | waiting room admit rate, checkout throughput, soak                                                              | —                                         | k6/Gatling                                                                            |
| **Chaos nhẹ**             | kill container giữa saga                                                                                        | —                                         | `docker kill`, script thủ công                                                        |

**Không test:** getter/setter, framework behavior, mapper tầm thường, mock đến mức test "mock gọi mock".

**Test transaction:** test rollback bằng cách ném exception giữa chừng và assert **không** có outbox event / dòng nửa vời. **Không** dùng `@Transactional` trên test class để "tự rollback" khi bạn đang test hành vi commit/concurrency (sẽ che bug).

------------------------------------------------------------------------

## 12. PERFORMANCE STRATEGY

Quy trình bắt buộc: `Baseline → Measure → Identify bottleneck → Optimize → Measure again → Ghi lại vào docs/learning/perf-journal.md`

| Bài toán                             | Cách tạo                                     | Công cụ đo                        | Hướng tối ưu (chỉ sau khi đo)                                                     |
|--------------------------------------|----------------------------------------------|-----------------------------------|-----------------------------------------------------------------------------------|
| Hot row contention trên `tier_stock` | 5.000 VU mua 1 tier                          | k6, `pg_stat_activity`, lock wait | atomic UPDATE → Redis Lua pre-check → chia stock thành N bucket (sharded counter) |
| N+1 ở danh sách order/event          | bật `hibernate.generate_statistics`, log SQL | log, test đếm query               | `JOIN FETCH`, `@EntityGraph`, DTO projection                                      |
| Query list chậm                      | seed 1–5 triệu dòng                          | `EXPLAIN (ANALYZE, BUFFERS)`      | composite/partial index, keyset pagination thay OFFSET                            |
| Cache miss storm khi event hot       | xóa cache + spike                            | hit ratio                         | cache-aside + TTL jitter + request coalescing                                     |
| Consumer lag                         | produce 100k event                           | lag metric                        | batch listener, partition, tối ưu idempotency write                               |
| Hikari cạn                           | giảm pool, tăng tải                          | `hikaricp_connections_pending`    | giảm thời gian giữ connection, không gọi mạng trong tx                            |
| Waiting room scale                   | 20k kết nối SSE                              | số connection, memory             | virtual threads (ADVANCED), tune Tomcat/Netty, heartbeat                          |

> **Không bịa số.** Mọi con số benchmark phải do bạn đo trên máy bạn, kèm cấu hình (CPU/RAM/pool/dataset). Mình chỉ cung cấp phương pháp.

**Benchmark methodology:** cố định dataset (seed script), warm-up 1–2 phút, chạy ≥3 lần lấy median, ghi p50/p95/p99 + error rate + CPU/DB metrics, **chỉ đổi 1 biến mỗi lần**.

------------------------------------------------------------------------

## 13. KNOWLEDGE MAP (map kiến thức → feature)

Nhãn: `CORE` · `USEFUL` · `ADVANCED` · `OPTIONAL` · `OVERENGINEERING`

### Java

| Kiến thức                                       | Nhãn     | Học ở đâu trong project                                  |
|-------------------------------------------------|----------|----------------------------------------------------------|
| OOP, Generics, Exception design                 | CORE     | domain model, error hierarchy                            |
| Collections, Stream, Optional                   | CORE     | mapping, aggregation báo cáo                             |
| Concurrency, ExecutorService, CompletableFuture | CORE     | concurrency test, outbox relay, parallel call có timeout |
| `synchronized`/`Lock`/atomic vs DB lock         | CORE     | hiểu vì sao lock JVM **vô dụng** khi có nhiều instance   |
| JVM: heap/GC, thread dump, heap dump            | USEFUL   | debug memory leak (bug \#4)                              |
| Virtual threads                                 | ADVANCED | waiting-room SSE                                         |

### Spring Boot

| Kiến thức                                                      | Nhãn            | Ở đâu                           |
|----------------------------------------------------------------|-----------------|---------------------------------|
| DI, bean lifecycle, `@Configuration`, profiles                 | CORE            | mọi service                     |
| Validation, `@RestControllerAdvice`                            | CORE            | error model chuẩn               |
| Spring Security (resource server, method security)             | CORE            | identity + mọi service          |
| `@Transactional` (propagation, rollback rule, self-invocation) | CORE            | order/inventory                 |
| JPA/Hibernate (lazy, N+1, `@Version`)                          | CORE            | catalog/order                   |
| `@Scheduled` + ShedLock                                        | CORE            | expire hold, reconciler, outbox |
| Actuator, Micrometer                                           | CORE            | observability                   |
| Spring Cloud Gateway                                           | USEFUL          | gateway                         |
| Spring Kafka (error handler, retry topic, DLT)                 | CORE            | consumers                       |
| Resilience4j (timeout, retry, CB, bulkhead)                    | USEFUL          | order→catalog/inventory         |
| Spring Cloud Config/Eureka                                     | OVERENGINEERING | —                               |

### API

REST, HTTP code, pagination (cursor), filter/sort, **idempotency**, error response (RFC 7807 problem+json), versioning (`/api/v1`), OpenAPI → **CORE**. HATEOAS → OVERENGINEERING. GraphQL → OPTIONAL (không cần).

### Database

SQL nâng cao, index (composite/partial/GIN), transaction & **isolation level**, optimistic/pessimistic lock, `SKIP LOCKED`, `EXPLAIN ANALYZE`, N+1, connection pool, Flyway → **CORE**. Partitioning, advisory lock → ADVANCED. Sharding → OVERENGINEERING.

### Distributed Systems

CAP/consistency model, eventual consistency, retry/timeout/backoff+jitter, idempotency, duplicate message, ordering (partition key), DLQ → **CORE**. Saga, Outbox/Inbox → **CORE**. Circuit breaker, bulkhead → USEFUL. CQRS-lite (read model) → USEFUL. Event sourcing → OVERENGINEERING (ở giai đoạn này). Consensus/Raft → OPTIONAL (đọc để hiểu, không tự cài).

### Architecture

Layered/Hexagonal-nhẹ, DDD cơ bản (aggregate, bounded context, ubiquitous language) → CORE. Microservices boundary, event-driven → CORE. API Gateway/BFF → USEFUL. Clean Architecture "đủ 4 vòng" cho mọi service → OVERENGINEERING.

### Infrastructure & Security

Docker, Compose, healthcheck, Kafka basics, Redis data structures, reverse proxy (Caddy), GitHub Actions → CORE/USEFUL. Terraform, K8s, Helm, Istio → OVERENGINEERING (hiện tại). JWT, refresh rotation, RBAC + ownership check, password hashing, CORS, CSRF, rate limit, secret mgmt → **CORE**. OAuth2/OIDC social login → OPTIONAL.

------------------------------------------------------------------------

## 14. TECHNOLOGY STACK

| Lớp              | Chọn                                                                     | Ghi chú                                                                           |
|------------------|--------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| Ngôn ngữ         | Java 21                                                                  | Records, pattern matching; virtual threads để dành sau                            |
| Framework        | Spring Boot 3.x                                                          |                                                                                   |
| Gateway          | Spring Cloud Gateway                                                     |                                                                                   |
| DB               | PostgreSQL 16 + Flyway                                                   | 1 instance Postgres, nhiều database/schema trong dev; **tách logic** nghiêm ngặt  |
| Cache/Queue ngắn | Redis 7                                                                  |                                                                                   |
| Broker           | Kafka (KRaft mode, không Zookeeper)                                      | Redpanda là phương án nhẹ hơn nếu máy yếu                                         |
| RPC              | gRPC (1 điểm)                                                            |                                                                                   |
| Real-time        | **SSE** (ưu tiên)                                                        | Một chiều server→client là đủ; WebSocket chỉ khi cần 2 chiều → hiện tại không cần |
| Auth             | Spring Security + JWT (RS256, JWKS)                                      | Không dùng Keycloak lúc đầu: bạn cần tự hiểu auth                                 |
| Frontend         | Next.js (App Router), TS, Tailwind, TanStack Query, react-hook-form, zod |                                                                                   |
| Test             | JUnit5, Mockito, Testcontainers, Playwright, k6                          |                                                                                   |
| Observability    | Micrometer, OpenTelemetry, Prometheus, Grafana, Jaeger                   |                                                                                   |
| CI/CD            | GitHub Actions + GHCR                                                    |                                                                                   |
| Deploy           | VPS + Docker Compose + Caddy (auto HTTPS) + Cloudflare                   |                                                                                   |

------------------------------------------------------------------------

## 15. KIẾN TRÚC TIẾN HÓA (microservices ngay từ đầu, nhưng không dựng tất cả cùng lúc)

Bạn muốn micro từ đầu → ta làm **Walking Skeleton** rồi **mỗi giai đoạn thêm 1 loại vấn đề phân tán**:

``` text
Stage A  Skeleton:   gateway + identity + catalog        (REST thuần, DB riêng)
Stage B  + Checkout: inventory + order (sync)           ← gặp: consistency, contention, dual-write
Stage C  + Async:    Kafka + Outbox + payment + ticket   ← gặp: duplicate, ordering, saga
Stage D  + Edge:     notification + audit + SSE          ← gặp: eventual consistency, read model
Stage E  + Scale:    waiting-room + Redis + rate limit   ← gặp: thundering herd, fairness
Stage F  + Resilience/Perf: CB, DLQ replay, chaos, tuning
```

| Chuyển giai đoạn | Vì sao cần đổi                                                                                | Vấn đề kiến trúc cũ không giải quyết tốt      | Kiến trúc mới giải quyết                   |
|------------------|-----------------------------------------------------------------------------------------------|-----------------------------------------------|--------------------------------------------|
| A → B            | Cần mua vé                                                                                    | Chưa có tranh chấp tài nguyên                 | Inventory độc lập: atomic update, hold TTL |
| B → C            | Order gọi sync mọi thứ → 1 service chậm là cả luồng chết; dual-write DB+gọi ngoài mất đồng bộ | Coupling thời gian, mất consistency khi crash | Event + Outbox + Saga                      |
| C → D            | Notification/audit gắn vào luồng chính làm chậm + dễ lỗi lây                                  | Side-effect không nên nằm trong critical path | Choreography consumers                     |
| D → E            | Lúc mở bán, traffic dồn → inventory/DB nghẽn, không công bằng                                 | Backpressure chỉ bằng rate limit là thô       | Waiting room điều tiết tốc độ vào          |
| E → F            | Có hệ thống rồi nhưng chưa biết nó **chịu lỗi** thế nào                                       | Tin vào happy path                            | Chaos + metrics + tuning                   |

------------------------------------------------------------------------

## 16. ROADMAP (phase-by-phase)

> Mỗi phase: **Goal · Features · Architecture · Tech · Concepts · Coding tasks (requirement, bạn tự nghĩ) · Difficulty · DoD · Common mistakes · Optimization · Interview**. Bộ phỏng vấn đầy đủ (10 lý thuyết + 10 project + 5 debug + 5 system design + 5 production) sẽ được tạo **sau mỗi phase** khi bạn báo xong; ở đây là câu mẫu.

Ước lượng thời gian (part-time ~12–15h/tuần, chỉ để định hướng): tổng ≈ 20–28 tuần.

------------------------------------------------------------------------

### PHASE 0 — Requirement & Skeleton design *(≈1 tuần)*

- **Goal:** Chốt scope, domain language, API contract đầu tiên, repo + infra tối thiểu.
- **Features:** Event storming 1 buổi (sticky note trên giấy/Excalidraw): liệt kê domain event của checkout; vẽ bounded context; viết 5 ADR đầu.
- **Architecture:** Monorepo, docker-compose (Postgres, Redis, Kafka), 1 service "hello" chạy qua gateway.
- **Concepts:** Bounded context, ubiquitous language, ADR, 12-factor.
- **Coding tasks:** Dựng repo/CI build; chuẩn hóa **error response (problem+json)**, correlation id filter, logging JSON, Flyway baseline; viết `docs/` khung.
- **Difficulty:** ★★☆☆☆
- **DoD:** `docker compose up` chạy được; CI xanh; ADR-001..005; có glossary.
- **Common mistakes:** Tạo shared lib nhét entity; cấu hình 1 DB dùng chung mọi service.
- **Optimization:** Cache layer của Maven/Gradle trong CI.
- **Interview mẫu:** Bounded context là gì? Vì sao mỗi service 1 DB? 12-factor app gồm những gì?

### PHASE 1 — Identity + Gateway *(≈2 tuần)*

- **Goal:** Auth chuẩn production.
- **Features:** Register/login, JWT RS256 + JWKS, refresh rotation + reuse detection, logout, RBAC, organizer profile, rate limit login.
- **Architecture:** gateway → identity; gateway verify JWT; propagate user context.
- **Concepts:** Spring Security filter chain, stateless auth, password hashing, CSRF/CORS, token family.
- **Coding tasks:** (1) Thiết kế bảng + flow refresh tự suy luận; (2) viết filter ở gateway xóa header giả mạo; (3) test: dùng lại refresh token cũ → cả family bị revoke; (4) method security cho endpoint admin.
- **Difficulty:** ★★★☆☆
- **DoD:** Test bảo mật pass (token hết hạn, sai chữ ký, reuse, role sai); không có secret trong repo; OpenAPI.
- **Common mistakes:** Lưu JWT trong localStorage; refresh token lưu plaintext; tin role từ request body; JWT sống quá lâu.
- **Optimization:** Cache JWKS có TTL; đo chi phí verify ở gateway.
- **Interview mẫu:** Access vs refresh token? Vì sao rotation? Logout JWT stateless thực sự làm gì? Privilege escalation là gì? BCrypt vs Argon2?

### PHASE 2 — Catalog + Workflow duyệt sự kiện *(≈2–3 tuần)*

- **Goal:** Domain có state machine + role-based workflow.
- **Features:** CRUD event/tier (ownership), submit→review→publish, status history, search FTS, filter, **cursor pagination**, staff assignment.
- **Architecture:** catalog service + Postgres; Redis cache-aside cho trang chi tiết (tạm thời, đo trước).
- **Concepts:** State pattern, JPA relationships, `@Version`, indexing, FTS, keyset pagination, ownership-based authorization.
- **Coding tasks:** State machine không dùng `if` rải rác; chặn "moderator duyệt event của chính mình"; viết migration index và chứng minh bằng `EXPLAIN`.
- **Difficulty:** ★★★☆☆
- **DoD:** Mọi transition sai trả `409/422` có mã lỗi; test ownership; list 1 triệu event vẫn nhanh bằng keyset (bạn đo).
- **Common mistakes:** OFFSET sâu; N+1 khi load tier; entity lộ ra API; validate chỉ ở UI.
- **Optimization:** Bài tập cố định: tạo N+1 → phát hiện → sửa → đo lại.
- **Interview mẫu:** OFFSET vs keyset? `@Version` hoạt động thế nào? Lazy loading gây N+1 ra sao? Index composite thứ tự cột quan trọng thế nào?

### PHASE 3 — Frontend nền (song song, ≈1–2 tuần rải rác)

- **Goal:** Có UI đủ để dùng thật 2 phase đầu.
- **Features:** Auth flow (silent refresh single-flight), list/search/detail, organizer tạo & submit, moderator duyệt.
- **Concepts:** App Router, middleware route guard, TanStack Query, zod, error mapping.
- **DoD:** Hết token → tự refresh không nhảy lỗi; lỗi validation backend hiện đúng field.
- **Mistakes:** Làm UI đẹp quá sớm; check quyền chỉ ở frontend.
- **Interview mẫu:** SSR vs CSR vs ISR? Vì sao không lưu token ở localStorage? Race khi nhiều request cùng gặp 401?

### PHASE 4 — Inventory (trái tim của hệ thống) *(≈3 tuần)*

- **Goal:** Không bao giờ oversell dưới tải đồng thời.
- **Features:** Stock theo tier, Reserve/Release/Commit (REST trước), hold TTL, worker expire, max_per_user, local replica từ `EventPublished`.
- **Architecture:** inventory + Postgres; consumer catalog events (chưa cần Kafka đầy đủ → có thể seed bằng REST rồi chuyển Kafka ở Phase 6, **hoặc** bật Kafka sớm — bạn quyết và ghi ADR).
- **Concepts:** Isolation level, lock strategies, atomic UPDATE, `SKIP LOCKED`, constraint như lớp phòng thủ, idempotent reserve.
- **Coding tasks:** Tự cài **ít nhất 2 chiến lược** (atomic update vs optimistic retry vs `FOR UPDATE`), viết concurrency test chung, đo throughput + lỗi từng cái, chọn 1 và ghi ADR.
- **Difficulty:** ★★★★☆
- **DoD:** Test 500 luồng/100 vé = đúng 100 (chạy 20 lần không flaky); expire worker chạy 2 instance không double-release; ADR so sánh có số đo của bạn.
- **Common mistakes:** Đọc-rồi-ghi (`SELECT` rồi `UPDATE` ở app); dùng `synchronized`; dùng thời gian app thay thời gian DB; quên release khi order fail.
- **Optimization:** Hot row → thử sharded counter (chỉ khi đo thấy nghẽn).
- **Interview mẫu:** Read Committed vs Repeatable Read khác gì ở bài này? Optimistic vs pessimistic khi contention cao? Vì sao `synchronized` không đủ? Deadlock xảy ra khi nào, tránh thế nào?

### PHASE 5 — Order + Saga đồng bộ rồi gRPC *(≈2–3 tuần)*

- **Goal:** Checkout hoàn chỉnh (sync) + hiểu dual-write.
- **Features:** Tạo order (Idempotency-Key), price snapshot, gọi inventory (REST → đo → **chuyển gRPC**), order state machine, cancel, order expire, reconciler.
- **Concepts:** Idempotency design, `@Transactional` không bọc call mạng, timeout/retry/CB, protobuf, deadline.
- **Coding tasks:** Mô phỏng crash giữa bước 2–3 (kill container) → viết reconciler; viết test double-submit 10 request song song cùng key → 1 order.
- **Difficulty:** ★★★★☆
- **DoD:** Không còn order "mồ côi"; mọi timeout có giá trị rõ ràng; so sánh REST vs gRPC bằng đo thực tế (không cảm tính).
- **Common mistakes:** Giữ transaction DB mở khi gọi gRPC; retry không idempotent; lưu giá theo tier hiện tại (không snapshot).
- **Optimization:** Connection pooling HTTP/gRPC channel; deadline propagation.
- **Interview mẫu:** Idempotency-Key lưu ở đâu, hết hạn sao? Dual-write là gì? Vì sao không để `@Transactional` bọc cả call gRPC? Circuit breaker 3 trạng thái?

### PHASE 6 — Kafka + Outbox + Payment + Ticket *(≈4 tuần — phase khó nhất)*

- **Goal:** Event-driven đúng nghĩa; saga bất đồng bộ.
- **Features:** Outbox relay mọi producer; payment-service + **mock provider** (delay/fail/duplicate webhook); webhook verify+dedupe; ticket issuance idempotent; order consume payment/ticket event; compensation (refund khi trễ); retry topic + DLQ + replay tool.
- **Concepts:** At-least-once, partition key & ordering, consumer group/rebalance, offset commit, Inbox, schema versioning, saga compensation.
- **Coding tasks:** Tự thiết kế envelope + topic; viết outbox poller `SKIP LOCKED`; test "kill consumer giữa chừng"; test duplicate event ×10; test thanh toán đến muộn.
- **Difficulty:** ★★★★★
- **DoD:** Chạy chaos script (kill Kafka/consumer/ticket-service) → cuối cùng mọi order đạt trạng thái cuối hợp lệ (COMPLETED/EXPIRED/REFUNDED), **không** vé mất, **không** vé trùng, **không** tiền treo.
- **Common mistakes:** Gửi Kafka trong `@Transactional` rồi tưởng đã atomic; consumer không idempotent; commit offset trước khi xử lý xong; dùng 1 partition rồi nói "có thứ tự".
- **Optimization:** Batch consumer; tăng partition; đo outbox latency.
- **Interview mẫu:** Exactly-once có thật? Vì sao cần Outbox? Order trong Kafka đảm bảo ở mức nào? Poison message xử lý ra sao? Saga vs 2PC? Event 2 lần thì sao?

### PHASE 7 — Notification, Audit, Real-time (SSE) *(≈2 tuần)*

- **Goal:** Side-effect & read model bằng choreography.
- **Features:** In-app + email (mock SMTP/Mailpit) với retry, SSE order status/notification, audit log append-only, ticket QR + check-in (idempotent, offline-friendly ở mức xử lý trùng).
- **Concepts:** SSE vs WebSocket, backpressure, reconnect/`Last-Event-ID`, local read model, eventual consistency UX.
- **DoD:** Tắt notification-service → checkout vẫn chạy; bật lại → thông báo được gửi bù, **không gửi trùng**.
- **Mistakes:** Gửi email trong luồng chính; SSE không heartbeat; rò quyền (user A nghe được stream của B).
- **Interview mẫu:** SSE vs WebSocket vs polling? Làm sao UI hiển thị trạng thái khi dữ liệu eventual? Tránh gửi email trùng thế nào?

### PHASE 8 — Waiting Room + Rate Limiting *(≈3 tuần)*

- **Goal:** Giải bài toán mở bán 5.000 người.
- **Features:** Join queue (Redis ZSET), admit worker (N người/chu kỳ, cấu hình theo event), SSE vị trí/ETA, admission token (ký, TTL, gắn user+event), inventory/order từ chối nếu thiếu token; heartbeat loại người rời; rate limit gateway (token bucket/sliding window).
- **Concepts:** Redis ZSET/Lua/TTL, fairness, backpressure, token bucket, fail-open vs fail-closed.
- **Coding tasks:** Quyết định & ghi ADR: Redis chết thì sao? Vào queue 2 tab thì sao? Làm sao chống một người chiếm nhiều chỗ?
- **DoD:** Load test: 5.000 VU vào phòng chờ, checkout vẫn ổn định trong ngưỡng bạn đặt; không ai bypass token.
- **Mistakes:** Admit theo "ai gọi API nhanh nhất"; không TTL → queue ma; Lua script không idempotent.
- **Interview mẫu:** Thiết kế rate limiter phân tán? Token bucket vs sliding window? Redis chết thì làm gì? Fairness đo thế nào?

### PHASE 9 — Observability + Resilience *(≈2 tuần)*

- **Goal:** Nhìn vào hệ thống trả lời được 5 câu hỏi ở mục 9.
- **Features:** Tracing end-to-end qua HTTP+gRPC+Kafka, dashboard, alert (consumer lag, outbox backlog, DLQ\>0, order kẹt), health/readiness chuẩn, chaos script.
- **DoD:** Từ 1 `correlationId` truy ra toàn bộ hành trình; kill 1 service → alert bắn trong ngưỡng.
- **Mistakes:** Log PII/token; cardinality label bùng nổ (userId làm metric label); alert quá nhiều.
- **Interview mẫu:** 3 trụ cột observability? Latency p99 vs average? Làm sao trace qua Kafka?

### PHASE 10 — Debugging Lab + Performance Tuning *(≈2–3 tuần)*

- **Goal:** Chuyển từ "biết làm" sang "biết sửa".
- **Features:** Làm lần lượt các bug ở mục 18; hoàn thành perf journal (baseline→optimize→đo lại) cho ≥4 bài toán ở mục 12.
- **DoD:** Mỗi bug có postmortem ngắn + test chống tái phát.

### PHASE 11 — Deployment & Public Release *(≈2 tuần)*

- **Goal:** Có link chạy thật.
- **Features:** CI/CD (build → test → image → deploy), staging→prod, backup Postgres, Caddy HTTPS, Cloudflare, seed demo, landing + "Flash Sale Simulator" (nút tạo 2.000 người ảo vào queue — demo cực kỳ ấn tượng), README + diagram + video demo.
- **DoD:** Tag `v1.0.0`; deploy bằng 1 lệnh/1 pipeline; rollback được; restore backup thử thành công.
- **Mistakes:** Mở port Postgres/Kafka ra Internet; không backup; secret trong image.

### PHASE 12 — Advanced Architecture *(tùy chọn, sau v1.0)*

- Xem mục 23 (v1.1 → v4).

------------------------------------------------------------------------

## 17. RECOMMENDED DEVELOPMENT ORDER

``` text
 1. Phase 0  Repo, CI, compose, error model, correlation id, logging, ADR 1–5
 2. Phase 1  identity + gateway           (auth chuẩn trước, đừng vá sau)
 3. Phase 2  catalog + workflow           (state machine, ownership)
 4. Phase 3  frontend nền (song song)     (login, list, organizer, moderator)
 5. Phase 4  inventory                    (concurrency core)  ← KHÔNG bỏ qua / KHÔNG rút gọn
 6. Phase 5  order (sync) + gRPC          (dual-write, reconciler)
 7. Phase 6  Kafka + outbox + payment + ticket (saga async)   ← phase khó nhất
 8. Phase 7  notification + audit + SSE + check-in
 9. Phase 8  waiting room + rate limit
10. Phase 9  observability + chaos
11. Phase 10 debug lab + perf
12. Phase 11 deploy + public
13. Phase 12 expansion (v1.1+)
```

**Vì sao Observability ở cuối?** Ngoài logging JSON + correlation id (làm từ Phase 0), phần dashboard/tracing đầy đủ chỉ có ý nghĩa khi đã có hệ thống phân tán để quan sát. Nhưng **health + metrics cơ bản bật từ phase 1**.

**Quy tắc "khóa phase":** chưa qua DoD + interview checkpoint của phase trước thì không sang phase sau.

------------------------------------------------------------------------

## 18. DEBUGGING MODE — bug cố tình đưa vào

Quy trình: `Observe → Reproduce → Investigate → Root cause → Fix → Test → Prevent regression → Viết postmortem 10 dòng`

| \#  | Bug (nhánh `bug/xx`)             | Mô tả triệu chứng                                                             | Kỹ năng luyện                              |
|-----|----------------------------------|-------------------------------------------------------------------------------|--------------------------------------------|
| 1   | Race condition inventory         | Thỉnh thoảng bán 101/100 vé khi tải cao                                       | concurrency test, DB lock, constraint      |
| 2   | N+1 ở `/me/orders`               | Trang chậm dần khi nhiều order                                                | log SQL, EntityGraph                       |
| 3   | Duplicate event                  | Vé phát hành 2 lần sau rebalance                                              | inbox, unique constraint                   |
| 4   | Memory leak                      | Waiting-room heap tăng không giảm (SSE emitter không remove)                  | heap dump, Eclipse MAT/VisualVM            |
| 5   | Slow query                       | List sự kiện chậm khi 2 triệu dòng, thiếu index/OFFSET sâu                    | `EXPLAIN ANALYZE`                          |
| 6   | Deadlock                         | 2 order mua nhiều tier theo thứ tự ngược → deadlock                           | `pg_locks`, lock ordering                  |
| 7   | Cache inconsistency              | UI hiện còn vé nhưng đã hết; giá cũ sau khi organizer sửa                     | cache invalidation theo event              |
| 8   | Retry storm                      | Inventory chậm → order retry → inventory càng chậm                            | backoff+jitter, CB                         |
| 9   | Consumer lag                     | Notification lag hàng chục nghìn event                                        | partition, batch, profiling                |
| 10  | Authentication bug               | Refresh token cũ vẫn dùng được / staff check-in sự kiện không được gán (IDOR) | security test, ownership check             |
| 11  | `@Transactional` self-invocation | Rollback không xảy ra, outbox ghi dù lỗi                                      | hiểu proxy AOP                             |
| 12  | Timezone bug                     | Mở bán lệch 7 tiếng                                                           | UTC everywhere, `Instant`/`OffsetDateTime` |

------------------------------------------------------------------------

## 19. INTERVIEW MODE (khung)

Sau mỗi major phase, mình sẽ tạo bộ: **10 lý thuyết · 10 project · 5 debugging · 5 system design · 5 production**. Dưới đây là câu hỏi "xương sống" bạn phải trả lời **bằng lý lẽ**, không học thuộc:

1.  Service chết giữa transaction thì sao? (DB rollback; còn side-effect ngoài DB thì sao?)
2.  Kafka gửi event 2 lần thì sao? (inbox/idempotent; vì sao không tin exactly-once?)
3.  2 user mua vé cuối cùng cùng lúc? (atomic update, isolation level, constraint)
4.  Redis chết thì sao? (từng use case: queue, rate limit, cache → phản ứng khác nhau)
5.  DB chậm thì làm gì? (đo → xác định → index/query/pool/cache; không "thêm RAM")
6.  Request bị gửi 5 lần? (Idempotency-Key)
7.  Vì sao không dùng distributed transaction? (blocking, coupling, không phải hệ nào cũng hỗ trợ; saga + compensation)
8.  Vì sao tách payment ra service riêng? Vì sao KHÔNG tách pricing?
9.  Webhook thanh toán đến sau khi hết hạn giữ chỗ?
10. Thiết kế hệ thống bán 10.000 vé trong 1 phút cho 100.000 người? (đây là đề system design "ruột" của project)

------------------------------------------------------------------------

## 20. WORKFLOW, DOCS, ADR

### 20.1 Git workflow

``` text
Requirement → Design (ADR/sơ đồ) → Issue → Branch → Code → Test → PR → Review (tự review bằng checklist) → CI → Merge → Deploy
```

- **Trunk-based nhẹ:** `main` luôn deploy được; branch ngắn `feat/…`, `fix/…`, `chore/…`, `docs/…`; tag `vX.Y.Z`.
- **Conventional Commits:** `feat(order): add idempotency key`, `fix(inventory): prevent negative stock`.
- **Issue template:** Context · Acceptance Criteria · Out of scope · Technical notes · Test plan.
- **PR checklist:** ☐ Có test (unit + integration nếu đụng DB/Kafka) ☐ Migration an toàn (backward compatible) ☐ Idempotent? ☐ Transaction boundary đúng? ☐ Có log/metric? ☐ Không lộ PII/secret ☐ Doc/ADR cập nhật ☐ Đã chạy lại concurrency test (nếu đụng inventory/order).
- **Code review checklist (tự review):** Ownership check? N+1? Gọi mạng trong transaction? Retry có idempotent? Timeout đã đặt? Lỗi có mã/ý nghĩa rõ? Có abstraction thừa?
- **Definition of Done (mỗi task):** chạy local bằng compose · test pass trong CI · không tăng warning · doc/ADR cập nhật · có metric/log phù hợp · demo được bằng 1 kịch bản.

### 20.2 Cấu trúc docs

``` text
README.md
docs/
├── architecture/     # c4 context/container, sequence diagram (checkout saga), event catalog
├── api/              # OpenAPI export, error codes, idempotency guide
├── database/         # ERD từng service + giải thích "vì sao"
├── decisions/        # ADR-0001-*.md …
├── deployment/       # compose, VPS, CI/CD, backup/restore, rollback
├── troubleshooting/  # runbook: "order kẹt PAID", "consumer lag", "DLQ có message"
└── learning/         # perf-journal.md, postmortems/, interview-notes/, bug-lab/
```

### 20.3 ADR — danh sách cần viết (mỗi cái: Context · Decision · Alternatives · Trade-offs · Consequences)

``` text
ADR-001 Vì sao microservices ngay từ đầu (mục tiêu học) và chấp nhận chi phí vận hành
ADR-002 Database-per-service, cấm cross-DB access
ADR-003 PostgreSQL là source of truth tồn kho; Redis chỉ tăng tốc
ADR-004 Orchestrated saga cho checkout, choreography cho side-effect
ADR-005 Transactional Outbox thay vì dual-write / 2PC
ADR-006 Atomic UPDATE vs optimistic vs pessimistic (kèm số đo)
ADR-007 SSE thay WebSocket
ADR-008 gRPC chỉ cho order→inventory
ADR-009 Redis chết: waiting-room fail-closed hay fail-open?
ADR-010 Thanh toán đến muộn: refund hay cứu order?
ADR-011 Refresh token rotation + reuse detection
ADR-012 Không dùng Eureka/Config Server/K8s
```

**Ví dụ ADR-005 (mẫu viết):**

- **Context:** Order cần ghi DB và phát event; crash giữa 2 việc → mất event hoặc event "ma".
- **Decision:** Ghi event vào bảng `outbox_events` cùng tx; relay đẩy Kafka.
- **Alternatives:** Dual-write trực tiếp (không atomic); 2PC/XA (blocking, Kafka không hỗ trợ); CDC Debezium (mạnh hơn nhưng thêm hạ tầng).
- **Trade-offs:** Thêm bảng + job; độ trễ vài trăm ms; at-least-once → consumer phải idempotent.
- **Consequences:** Mọi consumer có `processed_events`; cần metric outbox backlog; sau này có thể thay relay bằng Debezium mà không đổi domain code.

------------------------------------------------------------------------

## 21. DEPLOYMENT STRATEGY

``` text
Local (compose) → CI (test+build image) → Staging (VPS nhỏ/cùng VPS khác port) → Production (VPS + Caddy)
```

- **Chi phí thấp:** 1 VPS 4 vCPU/8GB (Hetzner/Contabo/DigitalOcean…) chạy Compose cho toàn bộ stack. Nếu RAM thiếu: gộp Postgres thành 1 instance nhiều DB, dùng Redpanda thay Kafka, tắt Loki.
- **Next.js:** Vercel/Cloudflare hoặc chạy container cùng VPS.
- **HTTPS/WAF/DNS:** Caddy + Cloudflare.
- **CI/CD:** GitHub Actions: lint → unit → integration (Testcontainers) → build image → push GHCR → SSH deploy `docker compose pull && up -d` → smoke test → rollback tag trước nếu fail.
- **Migration an toàn:** expand → migrate → contract (không đổi tên cột kiểu breaking).
- **Backup:** `pg_dump` định kỳ ra object storage + **thử restore**.
- **K8s:** Không cần. Chỉ cân nhắc khi: cần autoscale thật theo tải, nhiều node, rolling deploy phức tạp. Lúc đó bạn đã có Docker image sẵn → chuyển được.

------------------------------------------------------------------------

## 22. THINGS NOT TO BUILD & CẢNH BÁO OVERENGINEERING

### MUST HAVE (đạt Junior)

Auth chuẩn, state machine + ownership, inventory không oversell, idempotency, transaction đúng, Outbox/Inbox, saga cơ bản, Kafka consumer idempotent, index/EXPLAIN, Testcontainers + concurrency test, structured log + correlation id, Docker Compose, CI cơ bản.

### SHOULD HAVE

gRPC 1 điểm, Resilience4j, waiting room Redis, SSE, rate limit, tracing, k6, deploy VPS + backup.

### NICE TO HAVE

Redis Lua pre-check, sharded counter, partitioned audit table, Debezium, Pact, virtual threads.

### DO NOT BUILD YET

Kubernetes · Service mesh · Eureka/Config Server · Elasticsearch · Event Sourcing · GraphQL Federation · Saga framework (Axon/Temporal) · Kafka Streams · Multi-region · Tự viết payment thật · Social login · Mobile app · Recommendation/AI · Microfrontend · Tự viết identity thay thế OIDC chuẩn đầy đủ.

### Tín hiệu bạn đang đi lệch (tự kiểm tra hàng tuần)

| Dấu hiệu                                                   | Hành động                                                                            |
|------------------------------------------------------------|--------------------------------------------------------------------------------------|
| Thêm service thứ 11                                        | Dừng. Service mới phải có **lý do workload/ownership/failure domain** viết thành ADR |
| Dùng Kafka cho thứ 1 request/response đơn giản             | Đổi sang sync                                                                        |
| Tạo interface cho thứ chỉ có 1 implementation, "phòng khi" | Xóa                                                                                  |
| Tối ưu khi chưa có baseline                                | Dừng, đo trước                                                                       |
| Làm UI \> 25% thời gian                                    | Cắt UI                                                                               |
| Dành \> 25% thời gian cho DevOps/YAML                      | Quay lại backend fundamentals                                                        |
| `libs/common` \> vài trăm dòng hoặc chứa logic             | Tách trả về service                                                                  |
| Cố làm "exactly-once"                                      | Đọc lại mục 6.3                                                                      |
| Copy code AI/blog mà không giải thích được từng dòng       | Viết lại bằng tay + giải thích vào `docs/learning`                                   |

### Feature prioritization

| Feature                      | Value | Difficulty | Learning | Priority | Lý do                                                |
|------------------------------|------:|-----------:|---------:|---------:|------------------------------------------------------|
| Auth + refresh rotation      |     5 |          3 |        5 |   **P0** | Mọi thứ phụ thuộc; kiến thức phỏng vấn dày           |
| Catalog + workflow duyệt     |     4 |          2 |        4 |   **P0** | State machine + ownership, đủ nhẹ để ra sản phẩm sớm |
| Inventory + hold TTL         |     5 |          4 |        5 |   **P0** | Giá trị lõi + concurrency thật                       |
| Order + idempotency          |     5 |          4 |        5 |   **P0** | Dual-write, idempotency                              |
| Outbox + saga + payment mock |     5 |          5 |        5 |   **P0** | Phân tán thật; khó nhất                              |
| Ticket + check-in            |     4 |          3 |        4 |   **P1** | Hoàn thiện sản phẩm, bài toán duplicate scan         |
| Notification/Audit           |     3 |          2 |        4 |   **P1** | Choreography, tách side-effect                       |
| Waiting room                 |     5 |          4 |        5 |   **P1** | Điểm khác biệt, đẹp để demo                          |
| Rate limiting                |     4 |          3 |        4 |   **P1** | Security + performance                               |
| SSE real-time                |     3 |          3 |        4 |   **P1** | Frontend↔Backend thật                                |
| Refund/compensation          |     4 |          4 |        5 |   **P1** | Hoàn thiện saga                                      |
| Analytics read model         |     3 |          3 |        4 |       P2 | CQRS-lite                                            |
| Waitlist khi sold-out        |     3 |          4 |        4 |       P2 | Event-driven + fairness                              |
| Multi-tenant                 |     4 |          5 |        4 |       P3 | v4                                                   |

(Thang 1–5. "Learning" = mức kiến thức Junior học được. Priority = `Value × Learning` cao & phụ thuộc kỹ thuật đứng trước.)

------------------------------------------------------------------------

## 23. EXPANSION ROADMAP (mỗi version = bài toán kỹ thuật mới)

| Version  | Nội dung                                                                   | Bài toán kỹ thuật mới                                       |
|----------|----------------------------------------------------------------------------|-------------------------------------------------------------|
| **v1.0** | Workflow + checkout saga + QR check-in                                     | Consistency, idempotency, saga                              |
| **v1.1** | Notification đa kênh + email digest + preference                           | Retry, template, rate limit gửi, dedupe                     |
| **v2.0** | Waiting room + fairness + rate limit nâng cao                              | Thundering herd, Redis Lua, backpressure                    |
| **v2.1** | Real-time dashboard organizer, live check-in map                           | SSE scale, fan-out, read model                              |
| **v3.0** | Analytics pipeline (hourly/daily rollup), báo cáo xuất CSV/PDF bất đồng bộ | Batch/stream aggregation, file processing, job scheduling   |
| **v3.1** | Seat map (chọn ghế) + giữ ghế theo từng ghế                                | Locking mịn, bitmap/range lock, hot-spot                    |
| **v3.2** | Dynamic pricing / early-bird theo thời gian/tồn kho                        | Rule engine, tính nhất quán giá, cache                      |
| **v4.0** | Multi-tenant (mỗi tổ chức 1 workspace, plan, quota)                        | Tenant isolation, row-level security, per-tenant rate limit |
| **v4.1** | Debezium CDC thay outbox poller, schema registry                           | CDC, schema evolution                                       |
| **v5.0** | Kubernetes/autoscale, multi-AZ (chỉ khi có tải thật)                       | Orchestration, HA                                           |

------------------------------------------------------------------------

## 24. SKILL PROGRESSION: FRESHER → JUNIOR

``` text
Fresher ──► Strong Fresher ──► Junior ──► Strong Junior
```

| Level              | Biết gì                                                                                                                                       | Project giúp luyện              | Phase |
|--------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------|-------|
| **Fresher**        | CRUD, JPA cơ bản, REST, SQL cơ bản                                                                                                            | Phase 0–2                       |       |
| **Strong Fresher** | Auth đúng, transaction, index, test có DB thật, Docker                                                                                        | Phase 1–2, 4                    |       |
| **Junior**         | Giải thích & xử lý concurrency, idempotency, outbox, retry, event duplicate; đọc log/trace để debug; viết test đáng tin; review code của mình | Phase 4–7, 9–10                 |       |
| **Strong Junior**  | Tự đưa quyết định kiến trúc có trade-off (ADR), đo trước khi tối ưu, vận hành được hệ thống (deploy/backup/alert), chống overengineering      | Phase 8–11 + ADR + perf journal |       |

**Những gì project KHÔNG thay thế được kinh nghiệm thật:**

- Làm việc với codebase lớn/legacy do người khác viết.
- Incident thật lúc 2h sáng với dữ liệu thật và áp lực.
- Review/feedback từ đồng nghiệp có kinh nghiệm hơn (hãy xin người review repo của bạn, đăng lên cộng đồng).
- Tải & dữ liệu thật ở quy mô công ty; chi phí/vận hành lâu dài; làm việc theo team/deadline/ưu tiên mâu thuẫn.

→ Cách bù: public repo, mời review, chạy sự kiện thật với 1 CLB/nhóm bạn, viết postmortem.

------------------------------------------------------------------------

## 25. PUBLIC RELEASE & PORTFOLIO

- **Landing:** vấn đề (5 người tranh 1 vé cuối) → giải pháp (waiting room + hold + saga) → nút Demo.

- **Demo:** tài khoản demo 4 role; **Flash Sale Simulator** (tạo N user ảo vào phòng chờ, xem hàng đợi chảy, số vé về 0 mà không oversell) + dashboard Grafana công khai chế độ chỉ-xem.

- **README (cấu trúc):**

  ``` text
  Problem → Solution → Demo (GIF/video) → Architecture (diagram)
  → Tech stack & Why → Key challenges (Oversell, Dual-write, Late payment, Fairness)
  → Trade-offs & ADR links → Benchmark methodology & kết quả CỦA BẠN → How to run (1 lệnh) → Roadmap
  ```

- **Docs công khai:** OpenAPI (Swagger UI), event catalog, sequence diagram saga, ERD.

- **Blog kỹ thuật (3 bài gợi ý):** "Chống oversell: 4 cách và số đo", "Outbox: vì sao không dùng 2PC", "Webhook đến muộn: thiết kế compensation".

- **Rubric portfolio:** người xem phải trả lời được trong 3 phút: *bài toán gì → vì sao khó → bạn giải thế nào → bằng chứng (test/số đo) → điều bạn sẽ làm khác đi.*

------------------------------------------------------------------------

## 26. VẬN HÀNH KIỂU MENTOR (cách mình sẽ hỗ trợ bạn)

Với mỗi phase mình sẽ đưa: **Problem → Requirement → Gợi ý câu hỏi → (sau khi bạn thử) Solution + Trade-off**. Bạn tự triển khai; mình không code hộ trừ khi bạn yêu cầu.

Khi bạn gửi code/kiến trúc, mình sẽ phản hồi theo khung:

``` text
1. Bug  2. Root cause  3. Why it happens  4. Correct solution
5. Alternative  6. Trade-off  7. Production consideration  8. Kiến thức phỏng vấn liên quan
```

(và cảnh báo thẳng nếu code chạy nhưng kiến trúc sai, hoặc code tốt nhưng bạn đang overengineering.)

------------------------------------------------------------------------

# JUNIOR BACKEND SKILL CHECKLIST

Đánh dấu `[x]` chỉ khi bạn **tự giải thích được bằng lời** và **có test/bằng chứng** trong repo.

### Java & Spring

- [ ] Giải thích DI, bean scope/lifecycle, vì sao self-invocation làm `@Transactional` không chạy
- [ ] Thiết kế exception hierarchy + `@RestControllerAdvice` trả problem+json
- [ ] Dùng `ExecutorService`/`CompletableFuture` đúng (timeout, xử lý lỗi, không nuốt exception)
- [ ] Đọc thread dump/heap dump cơ bản, tìm 1 leak thật (bug \#4)
- [ ] Profiles, externalized config, không hardcode secret

### API

- [ ] Idempotency-Key cho POST nhạy cảm, có test song song
- [ ] Cursor pagination + filter/sort an toàn (chống injection, whitelist field)
- [ ] Error model thống nhất, versioning, OpenAPI chính xác
- [ ] Rate limit + trả `429 Retry-After`

### Database

- [ ] Giải thích & demo Read Committed vs Repeatable Read bằng 2 session
- [ ] Optimistic vs pessimistic lock — cài cả hai, đo cả hai
- [ ] Đọc `EXPLAIN (ANALYZE, BUFFERS)`, thêm index đúng, chứng minh bằng số
- [ ] Phát hiện & sửa N+1 có test đếm query
- [ ] Flyway migration backward-compatible
- [ ] Dùng `SKIP LOCKED` cho worker nhiều instance
- [ ] Hiểu connection pool, cấu hình có lý do

### Distributed systems

- [ ] Không còn dual-write (dùng Outbox) — có test crash giữa chừng
- [ ] Consumer idempotent — có test duplicate ×10
- [ ] Giải thích at-least-once, ordering theo partition key, rebalance
- [ ] Retry với backoff + jitter, DLQ + công cụ replay
- [ ] Circuit breaker/timeout/bulkhead — có test mô phỏng downstream chậm
- [ ] Saga có compensation (thanh toán muộn, phát hành vé fail)
- [ ] Giải thích vì sao không dùng 2PC; CAP & eventual consistency trong chính project này
- [ ] Local read model (replica từ event) và hiểu hệ quả

### Security

- [ ] Access/refresh token đúng, rotation + reuse detection
- [ ] Ownership/ABAC check ở service, không chỉ ở gateway
- [ ] Không tin dữ liệu danh tính từ client; chặn IDOR (có test)
- [ ] Webhook signature + dedupe; CORS/CSRF đúng; secret ngoài repo
- [ ] Password hashing đúng chuẩn

### Architecture

- [ ] Giải thích boundary & ownership từng service, vì sao không tách thêm
- [ ] Phân biệt orchestration/choreography và chọn có lý do
- [ ] ≥ 10 ADR có trade-off thật
- [ ] Phát hiện & gỡ ≥ 1 chỗ overengineering của chính mình

### Infra & Observability

- [ ] `docker compose up` chạy cả hệ thống; healthcheck/readiness đúng
- [ ] Trace 1 request qua HTTP → gRPC → Kafka bằng correlationId
- [ ] Dashboard: latency p95/p99, error rate, consumer lag, outbox backlog
- [ ] Alert cho DLQ, order kẹt, lag
- [ ] CI chạy unit + integration (Testcontainers); deploy tự động; backup + restore đã thử

### Testing & Performance

- [ ] Concurrency test chống oversell ổn định (chạy ≥ 20 lần không flaky)
- [ ] Integration test dùng Postgres/Kafka/Redis thật
- [ ] E2E happy path + ≥ 3 failure path
- [ ] Perf journal ≥ 4 bài: baseline → bottleneck → fix → đo lại
- [ ] k6 load test waiting room + checkout, kèm cấu hình môi trường

### Workflow & Communication

- [ ] Mọi thay đổi qua PR có checklist; commit theo convention
- [ ] Runbook troubleshooting ≥ 5 tình huống
- [ ] ≥ 5 postmortem từ bug lab
- [ ] README đạt rubric 3 phút; có ít nhất 1 người ngoài review repo
- [ ] Trả lời trôi chảy 10 câu "xương sống" ở mục 19 mà không cần nhìn tài liệu

------------------------------------------------------------------------

> **Bước tiếp theo:** bắt đầu **Phase 0**. Khi sẵn sàng, mình sẽ đưa Problem + Requirement chi tiết + bộ câu hỏi gợi mở cho Phase 0 (event storming checklist, ADR-001..005, cấu trúc repo/CI) — bạn tự làm, rồi gửi lại để mình review theo khung ở mục 26.
