# S09 — Observability

> Nguồn: mục "9. OBSERVABILITY".

## A. Mục tiêu
- Từ một `correlationId` truy ra hành trình request; trả lời 5 câu hỏi: đi qua service nào, chậm ở đâu, vì sao fail, Kafka delay ở đâu, query nào chậm.
- Liên hệ: S03 (gateway sinh correlation id), S06 (header Kafka), S10 (detect), Phase 9.

## B. Overview diagram

### S09-D1 — Năm câu hỏi → công cụ trả lời
```mermaid
flowchart LR
  Q1["Request đã đi qua những service nào?"] --> T1["Distributed tracing: Jaeger hoặc Tempo theo correlationId"]
  Q2["Nó chậm ở đâu?"] --> T2["Trace spans + metrics latency p50 p95 p99"]
  Q3["Vì sao request fail?"] --> T3["Structured log JSON lọc theo correlationId, Loki hoặc docker logs với jq"]
  Q4["Kafka event delay ở đâu?"] --> T4["consumer lag, DLQ size, outbox backlog"]
  Q5["Query DB nào chậm?"] --> T5["pg_stat_statements, slow query log"]
```

## C. Detailed diagrams

### S09-D2 — Lan truyền Correlation ID
```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant GW as Gateway
  participant O as order
  participant I as inventory
  participant K as Kafka
  participant T as ticket
  C->>GW: request, có thể chưa có X-Correlation-Id
  GW->>GW: sinh X-Correlation-Id nếu chưa có
  GW->>O: HTTP header X-Correlation-Id
  O->>I: gRPC metadata correlationId
  O->>K: Kafka header + envelope correlationId
  K->>T: consume, đọc correlationId từ header
  Note over GW,T: mọi dòng log có timestamp, level, service, correlationId, traceId, userId đã hash, eventId, orderId
```

### S09-D3 — Pipeline telemetry
```mermaid
flowchart LR
  SV["Các service"] -->|"log JSON"| LG["Logback JSON"]
  LG -.->|"thêm sau"| LK["Loki"]
  SV -->|"Micrometer metrics"| PR["Prometheus"]
  SV -->|"OpenTelemetry traces"| JG["Jaeger hoặc Tempo"]
  KE["kafka-exporter"] --> PR
  PGS["pg_stat_statements"] --> PR
  PR --> GF["Grafana"]
  JG --> GF
  LK -.-> GF
```
Để Compose nhẹ, bắt đầu bằng Prometheus + Grafana + Jaeger; Loki thêm sau.

### S09-D4 — Dashboard tối thiểu và business metrics
```mermaid
mindmap
  root((Dashboards))
    Golden signals từng service
      Latency p50 p95 p99
      Error rate
      RPS
      HikariCP pool
      JVM
    Saga funnel
      created
      reserved
      paid
      completed
      expired
    Kafka
      consumer lag
      DLQ size
      outbox backlog
    Queue
      Queue length
      Admit rate
    Business metrics
      orders_created
      holds_expired
      oversell_attempts_rejected
      queue_length
      checkin_duplicates
```

### S09-D5 — Health check
```mermaid
flowchart TB
  A["Actuator"] --> L["liveness: tiến trình còn sống"]
  A --> R["readiness: sẵn sàng nhận traffic"]
  R --> DB["kiểm tra DB"]
  R --> KF["kiểm tra Kafka"]
  R -->|"fail"| OUT["Ngừng nhận traffic, ví dụ readiness fail khi DB down"]
  Alert["Alert: consumer lag, outbox backlog, DLQ lớn hơn 0, order kẹt"] -.-> R
```

## D. Giải thích sơ đồ
- **D1** biến 5 câu hỏi của source thành "nhìn ở đâu"; nếu một câu hỏi không có công cụ trả lời thì phase 9 chưa xong.
- **D2** correlationId phải đi qua **cả ba** môi trường: HTTP header, gRPC metadata, Kafka header; thiếu một chỗ là đứt trace.
- **D3** hạ tầng quan sát gồm ba trụ: log, metric, trace.
- **D4** funnel saga là chỉ báo nghiệp vụ nhanh nhất cho thấy order kẹt ở đâu.

## E. Những điều phải nhớ
- Không log PII hoặc token (Phase 9 mistakes); userId hash.
- Không dùng userId làm label metric (cardinality bùng nổ).
- Health + metrics cơ bản bật từ Phase 1; logging JSON + correlation id từ Phase 0; dashboard/tracing đầy đủ ở Phase 9.
- **SOURCE CHƯA QUY ĐỊNH:** ngưỡng alert cụ thể, thời gian giữ log/metric, tên metric chính xác ngoài các business metric đã liệt kê, cách sampling trace, định dạng log field cụ thể.

## F. Liên kết
- Đầu vào: S03, S06. Đầu ra: S10 (Detect), S12 (Measure), S18 (debug), Phase 9.

## G. Checklist
- [x] structured logging  - [x] correlation/request id  - [x] metrics  - [x] health/readiness/liveness  - [x] tracing  - [x] 5 câu hỏi  - [x] 4 dashboard  - [x] business metrics
