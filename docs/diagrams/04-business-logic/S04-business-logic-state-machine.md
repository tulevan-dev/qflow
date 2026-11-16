# S04 — Business Logic & State Machine (4.1–4.5)

> Nguồn: mục "4. BUSINESS LOGIC & STATE MACHINE" (4.1 Event, 4.2 Order, 4.3 Hold, 4.4 Luồng checkout, 4.5 Race conditions).

## A. Mục tiêu
- Định nghĩa vòng đời Event / Order / Hold, ai được chuyển state và điều kiện.
- Mô tả luồng checkout 9 bước, 7 điểm đau (failure path) và 4 race condition chính.
- Liên hệ: S01 (role), S03 (service tham gia), S05 (bảng), S06 (outbox/inbox), S10 (failure scenarios).

## B. Overview diagram

### S04-D1 — Ba state machine và liên hệ giữa chúng
```mermaid
flowchart LR
  EV["Event<br/>catalog"] -->|"EventPublished / EventCancelled"| INV["Hold<br/>inventory"]
  ORD["Order<br/>order-service"] -->|"Reserve / Release / OrderPaid"| INV
  PAY["Payment events"] -->|"PaymentSucceeded"| ORD
  EV -->|"EventCancelled kích hoạt refund hàng loạt"| ORD
  INV -->|"hold hết hạn → order EXPIRED"| ORD
```

## C. Detailed diagrams

### S04-D2 — State machine: Event (catalog)
```mermaid
stateDiagram-v2
  [*] --> DRAFT
  DRAFT --> PENDING_REVIEW : submit bởi ORGANIZER chủ sự kiện
  PENDING_REVIEW --> PUBLISHED : approve bởi MODERATOR khác người tạo
  PENDING_REVIEW --> REJECTED : reject kèm lý do bắt buộc
  REJECTED --> DRAFT : edit và resubmit
  PUBLISHED --> ON_SALE : SYSTEM scheduler khi đến sale_starts_at
  ON_SALE --> SALE_CLOSED : sold out hoặc sale_ends
  SALE_CLOSED --> ENDED
  PUBLISHED --> CANCELLED : cancel bởi ORGANIZER hoặc ADMIN
  ON_SALE --> CANCELLED : cancel và phát EventCancelled
  CANCELLED --> [*]
  ENDED --> [*]
```
Điều kiện submit: đủ field, ≥1 tier, giờ hợp lệ, organizer đã VERIFIED.

### S04-D3 — State machine: Order (orchestrated saga)
```mermaid
stateDiagram-v2
  [*] --> PENDING
  PENDING --> AWAITING_PAYMENT : reserve OK
  PENDING --> REJECTED : reserve FAIL
  AWAITING_PAYMENT --> PAID : PaymentSucceeded
  AWAITING_PAYMENT --> CANCELLED : user cancel
  AWAITING_PAYMENT --> EXPIRED : hold expired
  EXPIRED --> REFUND_PENDING : PaymentSucceeded đến muộn
  PAID --> COMPLETED : TicketsIssued
  PAID --> REFUND_PENDING : issue FAIL hết retry
  REFUND_PENDING --> REFUNDED
  REJECTED --> [*]
  CANCELLED --> [*]
  COMPLETED --> [*]
  REFUNDED --> [*]
```

### S04-D4 — State machine: Hold (inventory)
```mermaid
stateDiagram-v2
  [*] --> ACTIVE
  ACTIVE --> CONSUMED : order PAID
  ACTIVE --> RELEASED : user cancel hoặc order failed
  ACTIVE --> EXPIRED : now lớn hơn expires_at do worker
  CONSUMED --> [*]
  RELEASED --> [*]
  EXPIRED --> [*]
```
Thời gian dùng để so sánh hết hạn là **thời gian DB** (`now()`), không phải thời gian ứng dụng (xem S10 #17).

### S04-D5 — Checkout happy path (9 bước)
```mermaid
sequenceDiagram
  autonumber
  actor A as Attendee
  participant GW as Gateway
  participant ORD as order
  participant INV as inventory
  participant PAY as payment
  participant MP as Mock Provider
  participant K as Kafka
  participant TKT as ticket
  participant NA as notification + audit
  A->>GW: POST /orders với Idempotency-Key và admission token nếu có waiting room
  GW->>ORD: chuyển tiếp
  ORD->>ORD: tx1 insert order PENDING
  ORD->>INV: gRPC Reserve orderId items ttl 10m idempotent theo orderId
  INV->>INV: tx UPDATE stock + INSERT hold + outbox
  INV-->>ORD: reserved
  ORD->>ORD: tx2 order AWAITING_PAYMENT + outbox OrderAwaitingPayment
  ORD-->>A: order AWAITING_PAYMENT
  A->>PAY: POST /payments với Idempotency-Key
  PAY-->>A: redirect sang Mock Provider
  MP->>PAY: webhook
  PAY->>PAY: verify signature, dedupe provider_event_id, tx payment SUCCEEDED + outbox PaymentSucceeded
  PAY->>K: PaymentSucceeded
  K->>ORD: consume
  ORD->>ORD: tx order PAID + outbox OrderPaid
  ORD->>K: OrderPaid
  K->>INV: consume
  INV->>INV: hold CONSUMED, reserved chuyển thành sold
  K->>TKT: consume
  TKT->>TKT: phát hành vé trong 1 transaction
  TKT->>K: TicketsIssued
  K->>ORD: consume
  ORD->>ORD: order COMPLETED
  K->>NA: mọi event được notification và audit consume
```

### S04-D6 — Failure #1: order crash sau reserve, trước update (điểm đau 1)
```mermaid
sequenceDiagram
  autonumber
  participant ORD as order
  participant INV as inventory
  participant REC as Reconciler SYSTEM
  ORD->>ORD: tx1 order PENDING
  ORD->>INV: Reserve orderId
  INV-->>ORD: reserved
  Note over ORD: crash trước tx2
  Note over INV: hold vẫn ACTIVE và có TTL
  REC->>ORD: quét order PENDING quá N giây
  REC->>INV: hỏi hold theo orderId
  alt hold còn ACTIVE
    REC->>ORD: sửa trạng thái hoặc hủy
  else hold đã EXPIRED
    REC->>ORD: order EXPIRED hoặc REJECTED
  end
  Note over REC: giá trị N source chưa quy định
```

### S04-D7 — Failure #2: webhook gửi nhiều lần (điểm đau 2)
```mermaid
sequenceDiagram
  autonumber
  participant MP as Mock Provider
  participant PAY as payment
  participant DB as payment DB
  MP->>PAY: webhook provider_event_id E1
  PAY->>DB: INSERT webhook_events E1
  DB-->>PAY: OK
  PAY->>DB: tx payment SUCCEEDED + outbox
  MP->>PAY: webhook E1 lần 2
  PAY->>DB: INSERT webhook_events E1
  DB-->>PAY: trùng PK provider_event_id
  PAY-->>MP: bỏ qua xử lý lặp
  Note over PAY,MP: HTTP code trả cho provider: SOURCE CHƯA QUY ĐỊNH
```

### S04-D8 — Failure #3: PaymentSucceeded đến sau khi order EXPIRED (điểm đau 3)
```mermaid
sequenceDiagram
  autonumber
  participant ORD as order
  participant INV as inventory
  participant PAY as payment
  participant K as Kafka
  INV->>INV: hold EXPIRED bởi worker
  ORD->>ORD: order EXPIRED
  PAY->>K: PaymentSucceeded
  K->>ORD: consume
  ORD->>ORD: state machine từ chối chuyển sang PAID
  alt Phương án A refund tự động
    ORD->>ORD: order REFUND_PENDING
    ORD->>K: RefundRequested
    K->>PAY: consume và hoàn tiền
    PAY->>K: RefundSucceeded
    K->>ORD: order REFUNDED
  else Phương án B cứu order nếu còn vé
    Note over ORD,INV: cần cơ chế xin lại stock
  end
  Note over ORD: quyết định chưa chốt, ghi ADR-010
```

### S04-D9 — Failure #4: ticket-service down sau khi đã thu tiền (điểm đau 7)
```mermaid
sequenceDiagram
  autonumber
  participant ORD as order
  participant K as Kafka
  participant TKT as ticket
  participant DLQ as DLQ
  ORD->>K: OrderPaid
  K-xTKT: consume thất bại vì ticket-service down
  Note over ORD: order kẹt PAID, consumer lag tăng
  K->>TKT: retry với backoff
  alt ticket-service sống lại
    TKT->>K: TicketsIssued
    K->>ORD: order COMPLETED
  else lỗi vĩnh viễn sau N lần retry
    K->>DLQ: đẩy vào DLQ, alert theo tuổi order
    ORD->>ORD: order REFUND_PENDING rồi REFUNDED
  end
```

### S04-D10 — Bảy điểm đau → cơ chế phòng thủ
```mermaid
flowchart LR
  P1["1 Order crash sau reserve"] --> M1["Hold có TTL + Reconciler quét PENDING"]
  P2["2 Webhook gửi 3 lần"] --> M2["PK provider_event_id + dedupe"]
  P3["3 Payment đến muộn sau EXPIRED"] --> M3["State machine từ chối + refund hoặc cứu order, ADR-010"]
  P4["4 User bấm Pay 5 lần"] --> M4["Idempotency-Key + unique constraint"]
  P5["5 Kafka down lúc nhận webhook"] --> M5["Outbox giữ event, relay gửi sau"]
  P6["6 OrderPaid xử lý 2 lần ở inventory"] --> M6["processed_events"]
  P7["7 Ticket-service down 30 phút"] --> M7["Retry + DLQ + alert"]
```

### S04-D11 — Race conditions chính
```mermaid
flowchart TB
  R1["2 request cùng trừ 1 vé cuối"] --> S1["Lựa chọn tự đánh giá"]
  S1 --> O1["a UPDATE ... WHERE available >= q atomic, chọn trước"]
  S1 --> O2["b optimistic lock + retry"]
  S1 --> O3["c SELECT FOR UPDATE"]
  S1 --> O4["d Redis Lua"]
  R2["Cùng user mua vượt max_per_user bằng 2 tab"] --> S2["unique hoặc atomic counter theo user và tier trong cùng tx"]
  R3["Expire worker chạy cùng lúc 2 instance"] --> S3["SELECT FOR UPDATE SKIP LOCKED hoặc ShedLock"]
  R4["User cancel đúng lúc payment thành công"] --> S4["version optimistic locking trên order + state machine từ chối transition sai"]
```

## D. Giải thích sơ đồ
- **D2 Event:** hai điều kiện quan trọng — người duyệt khác người tạo, reject bắt buộc có lý do. `PUBLISHED → ON_SALE` là việc của SYSTEM (scheduler), không phải người. Hủy sự kiện đã có vé bán phát `EventCancelled` kích hoạt refund hàng loạt.
- **D3 Order:** nhánh `EXPIRED → REFUND_PENDING` là trường hợp thanh toán đến muộn; `PAID → REFUND_PENDING` là khi phát hành vé thất bại hết retry. Cả hai kết thúc ở `REFUNDED`, đóng vòng bù trừ (compensation).
- **D4 Hold:** hold chỉ rời `ACTIVE` theo đúng 3 cách; đây là nguồn của "nhả vé tự động".
- **D5:** mỗi ghi DB đi kèm **outbox** cùng transaction; các bước 5–9 hoàn toàn bất đồng bộ qua Kafka nên không bắt user chờ. Bước reserve là sync vì user cần biết "giữ được vé hay không".
- **D6–D9:** mỗi sơ đồ là một failure path trong bảng 7 điểm đau; chúng đều quay về cùng nguyên tắc: *idempotent + TTL + outbox + state machine từ chối chuyển state sai*.
- **D11:** bốn race đều quy về "kiểm tra điều kiện và ghi phải là **một thao tác nguyên tử**" (hoặc có version) chứ không phải đọc-rồi-ghi ở tầng ứng dụng.

## E. Những điều phải nhớ
- Invariant: `reserved + sold ≤ total` (được bảo vệ bằng CHECK constraint, xem S05).
- Chỉ chuyển state theo đúng ma trận; transition sai trả `409/422` có mã lỗi (Phase 2 DoD).
- Order lưu **price snapshot**; đổi giá tier sau đó không ảnh hưởng order cũ.
- Không giữ transaction DB mở khi gọi gRPC (Phase 5).
- **Câu hỏi source để bạn tự nghĩ (chưa có đáp án):** Organizer sửa giá tier khi sự kiện ON_SALE? Đổi `total_quantity` xuống thấp hơn `sold`?
- **SOURCE CHƯA QUY ĐỊNH:** từ state nào được `CANCELLED` (bảng nói `*→CANCELLED`, sơ đồ ASCII chỉ vẽ từ PUBLISHED; D2 vẽ PUBLISHED và ON_SALE); ngưỡng N giây của reconciler; mã HTTP trả cho webhook trùng; quyết định refund vs cứu order (ADR-010).

## F. Liên kết
- Đầu vào: S01 (role), S03 (service). Đầu ra: S05 (schema hỗ trợ state/hold/outbox), S06 (event), S10 (kịch bản lỗi), Phase 4–6.

## G. Checklist
- [x] 4.1 Event  - [x] 4.2 Order  - [x] 4.3 Hold  - [x] 4.4 luồng 9 bước  - [x] 7 điểm đau (D6–D10)  - [x] 4.5 bốn race
