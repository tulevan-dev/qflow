# S06 — Message / Event Architecture (6.1–6.4)

> Nguồn: mục "6. MESSAGE / EVENT ARCHITECTURE" (6.1 Topics, 6.2 Envelope, 6.3 Bắt buộc, 6.4 Saga: orchestration hay choreography).

## A. Mục tiêu
- Xác định topic, key, event, producer/consumer và quy tắc bắt buộc (Outbox, Inbox, retry, DLQ, at-least-once).
- Giải thích vì sao checkout dùng orchestration, side-effect dùng choreography, và vì sao không dùng 2PC/XA.
- Liên hệ: S03 (D2 event backbone), S04 (saga), S05 (bảng outbox/processed_events), S10 (duplicate, poison, lag).

## B. Overview diagram

### S06-D1 — Đường đi của một event (từ ghi DB đến DLQ)
```mermaid
flowchart LR
  BIZ["Nghiệp vụ trong service"] -->|"cùng 1 transaction"| DB[("DB của service<br/>dữ liệu + outbox_events")]
  DB --> RELAY["Relay poller<br/>FOR UPDATE SKIP LOCKED"]
  RELAY -->|"publish, key = aggregateId"| TOPIC["Kafka topic"]
  TOPIC --> CONS["Consumer<br/>processed_events chống trùng"]
  CONS -->|"thành công"| OK["commit offset sau khi DB commit"]
  CONS -->|"lỗi tạm thời"| RETRY["retry topic + backoff"]
  RETRY --> CONS
  CONS -->|"lỗi vĩnh viễn hoặc hết N lần"| DLQ["topic .dlq"]
  DLQ -->|"sửa rồi replay"| TOPIC
```

## C. Detailed diagrams

### S06-D2 — Danh mục topic, key và event (6.1)
```mermaid
flowchart LR
  subgraph T1["catalog.events.v1 — key eventId"]
    E1["EventPublished<br/>EventCancelled<br/>TierChanged<br/>StaffAssigned"]
  end
  subgraph T2["inventory.events.v1 — key tierId"]
    E2["StockReserved<br/>ReservationRejected<br/>HoldExpired<br/>StockCommitted"]
  end
  subgraph T3["order.events.v1 — key orderId"]
    E3["OrderCreated<br/>OrderAwaitingPayment<br/>OrderPaid<br/>OrderCompleted<br/>OrderExpired<br/>OrderCancelled<br/>RefundRequested"]
  end
  subgraph T4["payment.events.v1 — key orderId"]
    E4["PaymentSucceeded<br/>PaymentFailed<br/>RefundSucceeded"]
  end
  subgraph T5["ticket.events.v1 — key orderId"]
    E5["TicketsIssued<br/>TicketCheckedIn"]
  end
  P1["catalog"] --> T1
  P2["inventory"] --> T2
  P3["order"] --> T3
  P4["payment"] --> T4
  P5["ticket"] --> T5
  T1 --> C1["inventory, ticket, notification, audit, waiting-room"]
  T2 --> C2["order, notification, audit"]
  T3 --> C3["payment, inventory, ticket, notification, audit"]
  T4 --> C4["order, notification, audit"]
  T5 --> C5["order, notification, audit"]
  C1 -.-> DLQ["*.dlq<br/>producer: consumer, replay bởi ADMIN hoặc SYSTEM"]
```

### S06-D3 — Envelope của mọi event (6.2)
```mermaid
classDiagram
  class EventEnvelope {
    +UUID eventId
    +String type
    +int version
    +Instant occurredAt
    +String aggregateId
    +String correlationId
    +String causationId
    +Object payload
  }
  class KafkaRecord {
    +String key
    +EventEnvelope value
  }
  KafkaRecord --> EventEnvelope : value
  note for EventEnvelope "eventId dùng để dedupe. version là schema version. correlationId để trace. causationId là event hoặc command gây ra nó"
  note for KafkaRecord "key = aggregateId để giữ thứ tự theo aggregate"
```

### S06-D4 — Transactional Outbox (6.3)
```mermaid
sequenceDiagram
  autonumber
  participant S as Service
  participant DB as DB của service
  participant R as Relay poller
  participant K as Kafka
  S->>DB: BEGIN, ghi dữ liệu nghiệp vụ + INSERT outbox_events, COMMIT
  loop mỗi chu kỳ
    R->>DB: SELECT outbox chưa published FOR UPDATE SKIP LOCKED
    DB-->>R: các dòng outbox
    R->>K: publish event
    K-->>R: ack
    R->>DB: đánh dấu published_at
  end
  Note over R,K: crash giữa publish và đánh dấu thì lần sau gửi lại
  Note over K: kết quả là at-least-once nên consumer phải idempotent
  Note over S,K: Kafka down thì event nằm lại trong outbox, relay gửi bù khi Kafka sống lại
```

### S06-D5 — Consumer idempotent bằng processed_events (Inbox)
```mermaid
flowchart TB
  M["Nhận message từ Kafka"] --> B["BEGIN transaction"]
  B --> I["INSERT processed_events eventId, consumer<br/>ON CONFLICT DO NOTHING"]
  I --> Q{"Chèn được dòng mới?"}
  Q -->|"có: chưa xử lý"| H["Xử lý nghiệp vụ trong cùng transaction"]
  H --> C["COMMIT"]
  C --> O["Commit offset"]
  Q -->|"không: đã xử lý trước đó"| S["Bỏ qua"]
  S --> C2["COMMIT hoặc rollback rỗng"]
  C2 --> O
  H -->|"exception"| RB["ROLLBACK, không commit offset"]
  RB --> RT["Retry hoặc DLQ"]
```

### S06-D6 — Retry và DLQ
```mermaid
flowchart TB
  F["Consumer xử lý lỗi"] --> T{"Loại lỗi?"}
  T -->|"tạm thời: DB chậm, network"| R["Retry topic với backoff"]
  R --> A{"Còn trong N lần?"}
  A -->|"còn"| P["Xử lý lại"]
  P --> F
  A -->|"hết N lần"| D["DLQ"]
  T -->|"vĩnh viễn: payload sai schema"| D
  D --> AL["Alert, DLQ lớn hơn 0"]
  AL --> FIX["Sửa nguyên nhân"]
  FIX --> RP["Replay từ DLQ bằng tooling"]
  Note1["N, backoff và tên retry topic: SOURCE CHƯA QUY ĐỊNH"] -.-> A
```

### S06-D7 — Orchestration cho checkout, choreography cho side-effect (6.4)
```mermaid
flowchart LR
  subgraph ORCH["Checkout = ORCHESTRATION"]
    O["order-service<br/>orchestrator, state machine rõ ràng"] -->|"gRPC Reserve"| I["inventory"]
    O -->|"OrderAwaitingPayment"| Pay["payment"]
    Pay -->|"PaymentSucceeded"| O
    O -->|"OrderPaid"| TK["inventory và ticket"]
    TK -->|"TicketsIssued"| O
  end
  subgraph CHOR["Notification, Audit, Analytics = CHOREOGRAPHY"]
    EV["Domain events"] --> N["notification"]
    EV --> A["audit"]
    EV --> AN["analytics read model"]
  end
  ORCH -.->|"phát event"| EV
```

### S06-D8 — Vì sao không dùng 2PC/XA
```mermaid
flowchart LR
  Need["Cần nhất quán giữa DB, Kafka và payment provider"] --> X["2PC / XA"]
  X --> X1["Blocking"]
  X --> X2["Coupling"]
  X --> X3["Kafka và payment provider không tham gia XA"]
  Need --> S["Saga + compensating action + eventual consistency"]
  S --> S1["Outbox cho ghi DB + event"]
  S --> S2["Idempotent consumer"]
  S --> S3["Refund khi cần bù trừ"]
```

## D. Giải thích sơ đồ
- **D1** tóm tắt toàn bộ cơ chế: event luôn đi qua DB (outbox) trước khi lên Kafka; mất Kafka không làm mất event.
- **D2** key = aggregate id giữ thứ tự **theo aggregate** (cùng orderId vào cùng partition); không đảm bảo thứ tự giữa các aggregate hay giữa các topic.
- **D3** `eventId` phục vụ dedupe, `version` phục vụ tiến hóa schema (chỉ thêm field optional), `correlationId`/`causationId` phục vụ truy vết.
- **D4** vì relay có thể crash sau khi publish, event có thể đi 2 lần → at-least-once; đó là lý do D5 bắt buộc.
- **D5** ghi dấu "đã xử lý" và xử lý nghiệp vụ trong **cùng transaction**, nên service chết giữa chừng thì cả hai cùng rollback; offset chỉ commit sau khi DB commit.
- **D6** phân biệt lỗi tạm thời (retry) và vĩnh viễn (DLQ ngay).
- **D7** orchestration cho thấy checkout đang kẹt ở bước nào; choreography cho các consumer chỉ nghe, không điều khiển ai.
- **D8** 2PC không khả thi vì thành phần tham gia không hỗ trợ XA và blocking; thay bằng saga.

## E. Những điều phải nhớ
- Delivery = **at-least-once**; đừng tin "exactly-once". Idempotent consumer mới là đáp án.
- Outbox bắt buộc ở **mọi** producer; Inbox bắt buộc ở **mọi** consumer.
- Schema event phải backward compatible; không đổi nghĩa field cũ.
- Dùng 1 partition rồi nói "có thứ tự" là lỗi phổ biến (Phase 6).
- Gửi Kafka trong `@Transactional` không làm nó atomic với DB.
- **SOURCE CHƯA QUY ĐỊNH:** số lần retry N, backoff, đặt tên retry topic/DLQ chi tiết, số partition, chính sách dọn `outbox_events`/`processed_events`, schema registry (chỉ nhắc ở v4.1), giữ thứ tự giữa các topic khác nhau.

## F. Liên kết
- Đầu vào: S03 (D2), S04 (saga), S05 (bảng). Đầu ra: S10 (#3, #7, #13, #14, #15), S11 (test duplicate/kill consumer), Phase 6.

## G. Checklist
- [x] 6.1 bảng topic  - [x] 6.2 envelope  - [x] 6.3 Outbox, Inbox, retry, DLQ, at-least-once, schema compat  - [x] 6.4 orchestration vs choreography, vì sao không 2PC
