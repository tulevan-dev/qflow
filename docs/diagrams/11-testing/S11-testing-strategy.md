# S11 — Testing Strategy

> Nguồn: mục "11. TESTING STRATEGY" (bảng 7 tầng, danh sách "không test", cách test transaction).

## A. Mục tiêu
- Xác định test cái gì, không test cái gì, mock ở đâu, dùng hạ tầng thật ở đâu, và cách test transaction/concurrency.
- Liên hệ: S04 (race/saga), S06 (idempotent consumer), S10 (kịch bản lỗi), S12 (load), Phase 4–6 (DoD gắn với test).

## B. Overview diagram

### S11-D1 — Bảy tầng test
```mermaid
flowchart TB
  U["Unit<br/>state machine, rule max_per_user, tính tiền, QR, mapper<br/>mock repo/client"] --> I["Integration service<br/>repository + Flyway, query phức tạp, outbox relay, consumer idempotent, controller + security<br/>KHÔNG mock DB/Kafka/Redis — Testcontainers"]
  I --> CC["Concurrency<br/>500 thread mua 100 vé, 2 worker expire, webhook gửi 10 lần song song"]
  CC --> CT["Contract<br/>schema event, gRPC proto; Pact OPTIONAL"]
  CT --> E["E2E<br/>Playwright + Docker Compose test profile; provider = mock-payment"]
  E --> L["Load<br/>waiting room admit rate, checkout throughput, soak — k6 hoặc Gatling"]
  L --> CH["Chaos nhẹ<br/>docker kill giữa saga, script thủ công"]
```

## C. Detailed diagrams

### S11-D2 — Test gì, không test gì
```mermaid
mindmap
  root((Testing))
    Nên test
      State machine
      Business rule
      Tiền và QR
      Repository và migration
      Outbox relay
      Consumer idempotent
      Security và ownership
      Concurrency
      Happy path E2E và failure path
    Không test
      Getter và setter
      Hành vi của framework
      Mapper tầm thường
      Test mock gọi mock
    Công cụ
      JUnit5
      Mockito
      AssertJ
      Testcontainers
      MockMvc hoặc WebTestClient
      Playwright
      k6 hoặc Gatling
```

### S11-D3 — Concurrency test chống oversell
```mermaid
sequenceDiagram
  autonumber
  participant T as Test
  participant P as ExecutorService 500 thread
  participant S as inventory trên Testcontainers Postgres
  T->>S: seed tier với total 100
  T->>P: tạo 500 task, cùng chờ CountDownLatch
  T->>P: mở latch, tất cả gọi Reserve cùng lúc
  P->>S: 500 request reserve song song
  S-->>P: thành công hoặc hết vé
  P-->>T: tổng kết quả
  T->>S: truy vấn tier_stock
  T->>T: assert đúng 100 thành công và 0 oversell
  T->>T: assert reserved + sold không vượt total
  Note over T,S: chạy lặp, DoD Phase 4: chạy 20 lần không flaky
```

### S11-D4 — Test transaction và rollback
```mermaid
flowchart TB
  S["Bắt đầu test hành vi commit"] --> X["Ném exception giữa chừng"]
  X --> A1["Assert không có dòng nửa vời"]
  X --> A2["Assert không có outbox event"]
  S --> N["KHÔNG dùng @Transactional trên test class<br/>vì tự rollback sẽ che bug commit và concurrency"]
```

### S11-D5 — Phạm vi E2E
```mermaid
flowchart LR
  PW["Playwright"] --> W["Next.js"]
  W --> GW["Gateway"]
  GW --> O["order"]
  O --> I["inventory"]
  O --> P["payment"]
  P --> MP["mock-payment provider"]
  P -.-> K[("Kafka")]
  K -.-> T["ticket"]
  K -.-> N["notification"]
  Scope["Happy path + ít nhất 3 failure path<br/>failure path cụ thể: SOURCE CHƯA QUY ĐỊNH"] -.-> PW
```

## D. Giải thích sơ đồ
- **D1** đi từ rẻ và nhanh đến đắt và chậm; mock chỉ ở unit. Integration dùng **hạ tầng thật** vì bug thật nằm ở ranh giới DB/Kafka/Redis.
- **D3** `CountDownLatch` làm mọi thread xuất phát cùng lúc để tạo contention thật; kiểm tra bất biến cuối cùng (đúng 100, không oversell).
- **D4** test rollback bằng cách phá giữa chừng và kiểm tra không rò dữ liệu/event; không bọc test trong transaction vì sẽ che hành vi commit.
- **D5** mock-payment là công cụ trong repo (`tools/mock-payment`: delay, fail, duplicate webhook).

## E. Những điều phải nhớ
- Không mock đến mức test "mock gọi mock".
- Concurrency test phải ổn định, nếu flaky thì chính nó là bug cần điều tra.
- Test duplicate event ×10, kill consumer giữa chừng, thanh toán đến muộn (Phase 6).
- **SOURCE CHƯA QUY ĐỊNH:** ngưỡng coverage, 3 failure path E2E cụ thể, có dùng Pact hay không (OPTIONAL), cấu hình CI chạy chaos.

## F. Liên kết
- Đầu vào: S04, S06, S10. Đầu ra: DoD các phase 4–6, S12 (load), S21 (CI).

## G. Checklist
- [x] unit/integration/concurrency/contract/E2E/load/chaos  - [x] không test  - [x] test transaction  - [x] Testcontainers, JUnit, Mockito
