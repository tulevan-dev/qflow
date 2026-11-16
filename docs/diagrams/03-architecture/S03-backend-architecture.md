# S03 — Kiến trúc Backend (3.1–3.7)

> Nguồn: mục "3. KIẾN TRÚC BACKEND" (3.1 Tổng quan, 3.2 Ranh giới service, 3.3 Ai gọi ai, 3.4 Sync vs Async, 3.5 Có cần không, 3.6 Cấu trúc code, 3.7 Monorepo).

## A. Mục tiêu
- Xác định 10 service, ranh giới, quyền sở hữu dữ liệu, ai được gọi ai và kiểu giao tiếp.
- Giải thích vì sao tách / không tách và thành phần nào KHÔNG dùng.
- Liên hệ: S05 (database), S06 (event), S07 (bảo mật), S15 (tiến hóa), S22 (cảnh báo overengineering).

## B. Overview diagram

### S03-D1 — Đường đi request (container view)
```mermaid
flowchart TB
  Client["Client"] --> Web["Next.js web"]
  Web -->|"HTTPS"| GW["API Gateway<br/>Spring Cloud Gateway<br/>JWT verify, rate limit, correlation id, CORS"]
  GW -.-> RGW[("Redis rate limit")]
  GW --> ID["identity-service"]
  GW --> CAT["catalog-service"]
  GW --> WR["waiting-room-service"]
  GW --> ORD["order-service<br/>saga orchestrator"]
  GW --> INV["inventory-service"]
  GW --> PAY["payment-service"]
  GW --> TKT["ticket-service<br/>và check-in"]
  GW --> NOTI["notification-service<br/>và SSE push"]
  GW --> AUD["audit-service"]
  ID --> DBID[("PG identity")]
  CAT --> DBCAT[("PG catalog")]
  WR --> RWR[("Redis")]
  ORD --> DBORD[("PG order")]
  INV --> DBINV[("PG inventory")]
  INV -.-> RINV[("Redis cache hiển thị")]
  PAY --> DBPAY[("PG payment")]
  TKT --> DBTKT[("PG ticket")]
  NOTI --> DBNOTI[("PG notification")]
  NOTI -.-> RNOTI[("Redis pub/sub")]
  AUD --> DBAUD[("PG audit")]
  ORD -->|"gRPC reserve / release"| INV
  ORD -->|"REST giá tier có cache + timeout + circuit breaker"| CAT
  MP["Mock Payment Provider"] -->|"webhook"| PAY
```

## C. Detailed diagrams

### S03-D2 — Event backbone (Kafka)
Chi tiết topic ở S06; sơ đồ này chỉ cho thấy phụ thuộc bất đồng bộ.
```mermaid
flowchart LR
  CAT["catalog"] --> T1["catalog.events.v1"]
  INV["inventory"] --> T2["inventory.events.v1"]
  ORD["order"] --> T3["order.events.v1"]
  PAY["payment"] --> T4["payment.events.v1"]
  TKT["ticket"] --> T5["ticket.events.v1"]
  T1 --> C1["inventory, ticket, notification, audit, waiting-room"]
  T2 --> C2["order, notification, audit"]
  T3 --> C3["payment, inventory, ticket, notification, audit"]
  T4 --> C4["order, notification, audit"]
  T5 --> C5["order, notification, audit"]
```

### S03-D3 — Vì sao mỗi service phải tách
```mermaid
mindmap
  root((10 service))
    api-gateway
      Điểm vào duy nhất
      Chặn traffic xấu trước service
      Lưu trữ Redis
    identity-service
      Bảo mật là bounded context riêng
      Blast radius lớn nếu lỗi
      PostgreSQL
    catalog-service
      Read-heavy đổi chậm
      Cần cache và search riêng
      Có workflow duyệt
      PostgreSQL
    waiting-room-service
      Nhiều kết nối SSE
      Ghi đọc Redis tốc độ cao
      Scale ngang lúc mở bán
      Redis
    inventory-service
      Nơi duy nhất quyết định còn vé hay không
      Consistency mạnh contention cao
      PostgreSQL source of truth
    order-service
      Điều phối checkout
      Nhạc trưởng của saga
      PostgreSQL
    payment-service
      Ranh giới với hệ thống ngoài
      Bắt buộc idempotent
      Cô lập dữ liệu nhạy cảm
      PostgreSQL
    ticket-service
      Check-in có burst tại cổng
      Phải chạy khi service khác chậm
      PostgreSQL
    notification-service
      Thuần consumer
      Lỗi gửi mail không làm hỏng checkout
      PostgreSQL và Redis
    audit-service
      Append-only
      Consume tất cả
      Không ai sửa được log
      PostgreSQL
```

### S03-D4 — Cố ý KHÔNG tách
```mermaid
flowchart LR
  PR["pricing"] --> CAT["catalog-service"]
  PM["promotion"] --> CAT
  VN["venue"] --> CAT
  SR["search"] --> CAT
  UP["user-profile"] --> ID["identity-service"]
  AN["analytics"] -->|"ban đầu"| AU["consumer nhỏ trong audit hoặc notification"]
  AN -.->|"chỉ tách khi có workload riêng, v3"| NEW["analytics service riêng"]
```

### S03-D5 — Ai được gọi ai
```mermaid
flowchart LR
  GW["Gateway"] -->|"REST"| ALL["mọi service"]
  ORD["order"] -->|"gRPC sync"| INV["inventory"]
  ORD -->|"REST sync + cache"| CAT["catalog"]
  MP["Mock Provider"] -->|"webhook"| PAY["payment"]
  EV["Mọi giao tiếp còn lại"] -->|"Kafka events async"| KF[("Kafka")]
  CAT -.->|"CẤM vòng gọi sync"| ORD
  SVC["Service A"] -.->|"CẤM đọc DB trực tiếp"| DBB[("DB của service B")]
```
Bốn quy tắc cứng (source):
1. Service **không** đọc DB của service khác; cần dữ liệu thì gọi API hoặc giữ local read model cập nhật qua event.
2. Không có vòng gọi sync (ví dụ `catalog → order → catalog`).
3. Ghi vào 2 nơi (DB + Kafka) **bắt buộc** qua Outbox.
4. Consumer **bắt buộc** idempotent (bảng `processed_events`).

### S03-D6 — Chọn sync hay async
```mermaid
flowchart TB
  Q["Cần giao tiếp giữa các service"] --> A1["Giữ vé khi bấm Mua"]
  Q --> A2["Lấy giá tier lúc tạo order"]
  Q --> A3["Thanh toán thành công → cập nhật order"]
  Q --> A4["Order paid → phát hành vé"]
  Q --> A5["Gửi email / notification"]
  Q --> A6["Audit log"]
  Q --> A7["Số vé còn lại cho UI"]
  A1 --> R1["gRPC sync order→inventory<br/>user cần biết ngay; bắt đầu bằng REST, đo, rồi đổi sang gRPC để học"]
  A2 --> R2["REST sync + cache<br/>chấp nhận stale ngắn vì order có snapshot giá"]
  A3 --> R3["Kafka event<br/>webhook đến lúc nào không biết, order có thể đang down"]
  A4 --> R4["Kafka event<br/>không bắt user chờ, retry được"]
  A5 --> R5["Kafka event<br/>không ảnh hưởng luồng chính"]
  A6 --> R6["Kafka event<br/>fire-and-forget nhiều consumer"]
  A7 --> R7["SSE hoặc poll từ read model<br/>eventual consistency; quyết định thật vẫn ở inventory"]
```

### S03-D7 — Thành phần: có cần không
```mermaid
flowchart LR
  subgraph YES["CÓ"]
    G["API Gateway<br/>rate limit, JWT, correlation id"]
    K["Kafka<br/>≥5 luồng event thật"]
    R["Redis có điểm dừng<br/>waiting room, rate limit, cache catalog"]
    GR["gRPC 1 chỗ duy nhất<br/>order→inventory"]
  end
  subgraph NO["KHÔNG — OVERENGINEERING"]
    SD["Service Discovery<br/>Docker Compose DNS đủ"]
    CC["Config Server<br/>env + Docker secrets + profiles"]
    ES["Elasticsearch<br/>Postgres FTS + pg_trgm đủ"]
    K8["Kubernetes<br/>Compose trên VPS"]
    SM["Service Mesh"]
  end
```
Ghi chú source: Redis **không** làm source of truth tồn kho ở bản đầu.

### S03-D8 — Cấu trúc code trong mỗi service (Hexagonal-nhẹ)
```mermaid
flowchart TB
  API["api<br/>controller, DTO, exception handler"] --> APP["application<br/>use case, transaction boundary"]
  APP --> DOM["domain<br/>entity, value object, state machine, domain event, rule thuần"]
  INFRA["infrastructure<br/>JPA repo, Kafka producer/consumer, gRPC/REST client, Redis"] --> DOM
  APP --> INFRA
  CFG["config"] -.-> API
  CFG -.-> INFRA
```

### S03-D9 — Monorepo
```mermaid
mindmap
  root((qflow))
    services
      gateway
      identity
      catalog
      waiting-room
      inventory
      order
      payment
      ticket
      notification
      audit
    libs
      event-contracts chỉ schema event và envelope versioned
      common-web error model và correlation filter mỏng
    web Next.js
    tools
      mock-payment delay fail duplicate webhook
      load k6 scripts
    infra
      compose
      grafana
      prometheus
      caddy
    docs
```

## D. Giải thích sơ đồ
- **D1:** mọi request ngoài đi qua **một** cổng (gateway). Gateway xác thực thô và giới hạn tốc độ rồi chuyển tới service; mỗi service chỉ nói chuyện với DB của mình. Hai đường sync nội bộ duy nhất: `order → inventory` (gRPC) và `order → catalog` (REST có cache/timeout/circuit breaker). Webhook từ provider đi thẳng vào payment.
- **D2:** thay vì gọi nhau, các service công bố sự kiện lên topic của mình; consumer đăng ký theo bảng 6.1. Nhờ đó notification/audit chậm hoặc chết không ảnh hưởng checkout.
- **D3–D4:** mỗi service tách vì **workload, tính nhất quán, failure domain hoặc ranh giới ngoài** khác nhau; thứ chưa đủ lý do (pricing, search…) nằm trong service đã có để tránh quá nhiều service.
- **D5:** cạnh đứt nét là các điều **bị cấm**: vòng gọi sync và truy cập DB chéo; cả hai đều dẫn tới distributed monolith.
- **D6:** nguyên tắc: người dùng cần câu trả lời ngay → sync; việc phụ hoặc việc có thể trễ → event.
- **D7:** chỉ dùng thành phần mà bài học thực sự cần; còn lại ghi rõ là overengineering.
- **D8–D9:** cấu trúc code/monorepo giữ mỏng; `libs/` chỉ chứa schema event và tiện ích web, **không** chứa logic nghiệp vụ.

## E. Những điều phải nhớ
- Distributed monolith = `libs/` phình to hoặc service đọc DB của nhau.
- Domain không import annotation Spring/JPA nặng nề nếu tránh được, nhưng **đừng** tách "domain model ≠ JPA entity" ngay từ đầu (abstraction quá sớm).
- Dev dùng 1 instance Postgres nhiều database/schema nhưng **tách logic nghiêm ngặt**.
- **SOURCE CHƯA QUY ĐỊNH:** đường đi của webhook qua gateway hay trực tiếp (sơ đồ source vẽ trực tiếp); phạm vi API công khai của inventory/audit dù source nói "Gateway → mọi service"; cách xác thực service-to-service ngoài việc nhắc machine token cho SYSTEM.

## F. Liên kết
- Đầu vào: S01, S02. Đầu ra: S04 (luồng nghiệp vụ trên các service), S05 (ownership dữ liệu), S06 (topic), S07 (trust boundary), S15, S21.

## G. Checklist
- [x] 3.1 tổng quan  - [x] 3.2 bảng 10 service + "không tách"  - [x] 3.3 ai gọi ai + 4 quy tắc  - [x] 3.4 bảng sync/async  - [x] 3.5 bảng có cần không  - [x] 3.6 hexagonal-nhẹ  - [x] 3.7 monorepo
