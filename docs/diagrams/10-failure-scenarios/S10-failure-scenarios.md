# S10 — Real-World Failure Scenarios

> Nguồn: mục "10. REAL-WORLD FAILURE SCENARIOS" (19 tình huống, mỗi tình huống theo Problem → Why → Detect → Prevent → Recover → Trade-off).

## A. Mục tiêu
- Chuyển 19 tình huống lỗi thành sơ đồ chuỗi nguyên nhân–phát hiện–phòng–phục hồi–đánh đổi để dùng khi thiết kế, test (S11) và debug (S18).
- Liên hệ: S04 (failure sequence #5, #9, #11 đã vẽ ở S04-D6, D8, D9), S06 (outbox/inbox/DLQ), S07 (#10, #19), S09 (Detect), S12 (#16).

## B. Overview diagram

### S10-D0 — 19 tình huống theo nhóm
```mermaid
mindmap
  root((19 failure scenarios))
    Hạ tầng sập
      1 Postgres inventory down
      2 Redis down
      3 Kafka down
    Mạng và trùng lặp
      4 Timeout order đến inventory
      6 Duplicate request
      7 Duplicate event
    Đồng thời và nhất quán
      8 Concurrent update
      17 Clock skew và TTL
      18 Cache inconsistency
    Thanh toán và saga
      5 Order crash sau reserve
      9 Webhook đến sau hold hết hạn
      10 Webhook giả mạo
      11 Ticket-service down sau khi thu tiền
      12 Partial failure phát hành vé
    Kafka consumer
      13 Consumer lag
      14 Poison message
      15 Restart giữa lúc xử lý
    Tải và bot
      16 Retry storm
      19 Bot và thundering herd
```

## C. Detailed diagrams
Mỗi sơ đồ nhóm dưới đây giữ **đủ 6 trường** của từng tình huống theo đúng bảng source. Chỗ nào source ghi `—` thì ghi là "source không nêu".

### S10-D1 — Nhóm G1: Hạ tầng sập
```mermaid
flowchart TB
  subgraph SC1["#1 Postgres inventory down"]
    direction TB
    p1["Problem: Postgres inventory down"] --> w1["Why: crash hoặc hết connection"] --> d1["Detect: readiness fail, Hikari pending tăng"] --> pr1["Prevent: pool size hợp lý, timeout ngắn, circuit breaker ở order"] --> r1["Recover: order trả 503 + Retry-After, không tạo order nửa vời"] --> t1["Trade-off: chọn fail fast thay vì cho đặt tạm (mất consistency)"]
  end
  subgraph SC2["#2 Redis down"]
    direction TB
    p2["Problem: Redis down"] --> w2["Why: OOM hoặc network"] --> d2["Detect: health check, error rate"] --> pr2["Prevent: waiting-room: fail-closed hay fail-open? (tự quyết, ADR-009). Rate limit: fail-open có giới hạn local. Cache: fallback DB"] --> r2["Recover: khởi động lại; rebuild queue từ đâu? (source để bạn tự nghĩ)"] --> t2["Trade-off: availability vs fairness"]
  end
  subgraph SC3["#3 Kafka down"]
    direction TB
    p3["Problem: Kafka down"] --> w3["Why: broker chết"] --> d3["Detect: producer error, outbox backlog tăng"] --> pr3["Prevent: Outbox: ghi DB trước"] --> r3["Recover: relay tự gửi bù khi Kafka sống lại"] --> t3["Trade-off: checkout vẫn chạy nhưng hoàn tất chậm (eventual)"]
  end
```

### S10-D2 — Nhóm G2: Mạng và trùng lặp
```mermaid
flowchart TB
  subgraph SC4["#4 Network timeout order → inventory"]
    direction TB
    p4["Problem: Network timeout order → inventory"] --> w4["Why: mạng hoặc GC pause"] --> d4["Detect: timeout metric"] --> pr4["Prevent: timeout + retry với cùng orderId (idempotent)"] --> r4["Recover: gọi lại Reserve; kết quả 'trạng thái không biết' thì query hold theo orderId"] --> t4["Trade-off: retry có thể gây tải thêm (retry storm) nên cần backoff + jitter"]
  end
  subgraph SC6["#6 Duplicate request (double click, retry mạng)"]
    direction TB
    p6["Problem: Duplicate request (double click, retry mạng)"] --> w6["Why: client hoặc proxy retry"] --> d6["Detect: unique violation (user, idempotency_key)"] --> pr6["Prevent: Idempotency-Key + unique constraint"] --> r6["Recover: trả lại kết quả cũ (cùng body, 200 hoặc 201)"] --> t6["Trade-off: lưu key thêm storage, cần TTL cleanup"]
  end
  subgraph SC7["#7 Duplicate event, xử lý 2 lần"]
    direction TB
    p7["Problem: Duplicate event, xử lý 2 lần"] --> w7["Why: at-least-once, rebalance"] --> d7["Detect: processed_events conflict"] --> pr7["Prevent: idempotent consumer"] --> r7["Recover: bỏ qua"] --> t7["Trade-off: thêm 1 write mỗi event"]
  end
```

### S10-D3 — Nhóm G3: Đồng thời và nhất quán dữ liệu
```mermaid
flowchart TB
  subgraph SC8["#8 Concurrent update cùng resource"]
    direction TB
    p8["Problem: Concurrent update cùng resource"] --> w8["Why: 2 transaction đọc cũ rồi ghi đè"] --> d8["Detect: OptimisticLockException"] --> pr8["Prevent: @Version, atomic UPDATE"] --> r8["Recover: 409 + client refetch, hoặc retry có giới hạn"] --> t8["Trade-off: retry nhiều khi contention cao"]
  end
  subgraph SC17["#17 Clock skew và TTL"]
    direction TB
    p17["Problem: Clock skew và TTL"] --> w17["Why: nhiều node lệch giờ"] --> d17["Detect: hold hết hạn sai"] --> pr17["Prevent: dùng thời gian DB (now()) cho expiry"] --> r17["Recover: source không nêu"] --> t17["Trade-off: source không nêu"]
  end
  subgraph SC18["#18 Cache inconsistency (giá hoặc availability cũ)"]
    direction TB
    p18["Problem: Cache inconsistency (giá hoặc availability cũ)"] --> w18["Why: invalidate trễ"] --> d18["Detect: so sánh cache với DB"] --> pr18["Prevent: TTL ngắn, invalidate theo event, quyết định ghi luôn ở DB"] --> r18["Recover: xóa cache"] --> t18["Trade-off: stale để hiển thị chấp nhận được, stale để quyết định thì không"]
  end
```

### S10-D4 — Nhóm G4: Thanh toán và saga
```mermaid
flowchart TB
  subgraph SC5["#5 Order crash sau reserve, trước update"]
    direction TB
    p5["Problem: Order crash sau reserve, trước update"] --> w5["Why: process bị kill"] --> d5["Detect: order kẹt PENDING quá N giây"] --> pr5["Prevent: hold có TTL"] --> r5["Recover: Reconciler: quét PENDING cũ, hỏi inventory, sửa trạng thái hoặc hủy"] --> t5["Trade-off: thêm 1 job nền phải vận hành"]
  end
  subgraph SC9["#9 Webhook thanh toán đến sau khi hold hết hạn"]
    direction TB
    p9["Problem: Webhook thanh toán đến sau khi hold hết hạn"] --> w9["Why: mạng chậm, user trả muộn"] --> d9["Detect: PaymentSucceeded cho order EXPIRED"] --> pr9["Prevent: state machine từ chối"] --> r9["Recover: hoàn tiền tự động hoặc cứu order nếu stock còn (quyết định + ADR-010)"] --> t9["Trade-off: UX vs độ phức tạp"]
  end
  subgraph SC10["#10 Webhook giả mạo"]
    direction TB
    p10["Problem: Webhook giả mạo"] --> w10["Why: attacker gọi endpoint"] --> d10["Detect: signature invalid"] --> pr10["Prevent: HMAC + timestamp + IP allowlist (optional)"] --> r10["Recover: log + alert"] --> t10["Trade-off: thêm chi phí verify"]
  end
  subgraph SC11["#11 Ticket-service down sau khi đã thu tiền"]
    direction TB
    p11["Problem: Ticket-service down sau khi đã thu tiền"] --> w11["Why: crash hoặc deploy"] --> d11["Detect: order kẹt PAID, consumer lag"] --> pr11["Prevent: retry vô hạn có backoff, DLQ, alert theo tuổi order"] --> r11["Recover: replay; nếu vĩnh viễn thì refund"] --> t11["Trade-off: thời gian user chờ vé"]
  end
  subgraph SC12["#12 Partial failure phát hành vé (3/5)"]
    direction TB
    p12["Problem: Partial failure phát hành vé (3/5)"] --> w12["Why: lỗi giữa vòng lặp"] --> d12["Detect: số ticket khác quantity"] --> pr12["Prevent: phát hành trong 1 transaction + UNIQUE(order_id, tier_id, seq)"] --> r12["Recover: chạy lại idempotent"] --> t12["Trade-off: transaction dài hơn"]
  end
```

### S10-D5 — Nhóm G5: Kafka consumer
```mermaid
flowchart TB
  subgraph SC13["#13 Consumer chậm gây lag"]
    direction TB
    p13["Problem: Consumer chậm gây lag"] --> w13["Why: xử lý nặng, DB chậm"] --> d13["Detect: lag metric"] --> pr13["Prevent: tăng partition và consumer, batch, tối ưu query"] --> r13["Recover: scale consumer"] --> t13["Trade-off: thứ tự theo key vẫn phải giữ"]
  end
  subgraph SC14["#14 Poison message"]
    direction TB
    p14["Problem: Poison message"] --> w14["Why: payload hỏng"] --> d14["Detect: lỗi lặp cùng offset"] --> pr14["Prevent: validate schema, DLQ sau N lần"] --> r14["Recover: sửa rồi replay từ DLQ"] --> t14["Trade-off: cần tooling replay"]
  end
  subgraph SC15["#15 Service restart giữa lúc xử lý Kafka message"]
    direction TB
    p15["Problem: Service restart giữa lúc xử lý Kafka message"] --> w15["Why: deploy"] --> d15["Detect: message được giao lại"] --> pr15["Prevent: commit offset sau khi transaction DB commit"] --> r15["Recover: idempotent consumer"] --> t15["Trade-off: source không nêu"]
  end
```

### S10-D6 — Nhóm G6: Tải và bot
```mermaid
flowchart TB
  subgraph SC16["#16 Retry storm"]
    direction TB
    p16["Problem: Retry storm"] --> w16["Why: mọi client retry đồng loạt khi service chậm"] --> d16["Detect: RPS tăng đột ngột + 5xx"] --> pr16["Prevent: exponential backoff + jitter, circuit breaker, 429 Retry-After"] --> r16["Recover: bulkhead, shed load"] --> t16["Trade-off: từ chối bớt request để cứu hệ thống"]
  end
  subgraph SC19["#19 Bot hoặc thundering herd"]
    direction TB
    p19["Problem: Bot hoặc thundering herd"] --> w19["Why: script spam"] --> d19["Detect: rate limit hit, queue join bất thường"] --> pr19["Prevent: waiting room + rate limit + giới hạn mỗi người"] --> r19["Recover: block, tăng PoW"] --> t19["Trade-off: friction cho người thật"]
  end
```

### S10-D7 — #4 Timeout order → inventory và retry cùng orderId
```mermaid
sequenceDiagram
  autonumber
  participant O as order
  participant I as inventory
  O->>I: gRPC Reserve orderId O1
  I->>I: tx reserve + hold, UK order_id tier_id
  Note over O,I: timeout hoặc GC pause, order không biết kết quả
  O->>O: ghi nhận trạng thái không biết
  O->>I: retry Reserve cùng orderId O1 với backoff + jitter
  alt hold O1 đã tạo
    I-->>O: trả kết quả cũ, không trừ thêm vé
  else chưa tạo
    I->>I: tạo hold mới
    I-->>O: reserved
  end
  Note over O,I: hoặc truy vấn hold theo orderId trước khi quyết định
```

### S10-D8 — #6 Duplicate request được chặn bằng Idempotency-Key
```mermaid
sequenceDiagram
  autonumber
  participant C as Client
  participant O as order
  participant DB as order-db
  C->>O: POST /orders, Idempotency-Key K1
  O->>DB: INSERT orders, UK user_id idempotency_key
  DB-->>O: OK
  O-->>C: 201 + order
  C->>O: gửi lại cùng K1 do double click hoặc retry mạng
  O->>DB: INSERT orders, UK user_id idempotency_key
  DB-->>O: unique violation
  O->>DB: đọc order cũ theo user_id và K1
  O-->>C: trả kết quả cũ, cùng body, 200 hoặc 201
  Note over O,DB: key cần TTL cleanup, SOURCE CHƯA QUY ĐỊNH thời gian giữ
```

### S10-D9 — #7 Duplicate event và rebalance
```mermaid
sequenceDiagram
  autonumber
  participant K as Kafka
  participant C1 as Consumer A
  participant C2 as Consumer B
  participant DB as DB service
  K->>C1: OrderPaid E1
  C1->>DB: tx INSERT processed_events E1 + xử lý nghiệp vụ, COMMIT
  Note over C1,K: rebalance trước khi commit offset
  K->>C2: OrderPaid E1 giao lại
  C2->>DB: INSERT processed_events E1
  DB-->>C2: conflict
  C2->>C2: bỏ qua, commit offset
```

### S10-D10 — #12 Partial failure phát hành vé (3 trên 5)
```mermaid
sequenceDiagram
  autonumber
  participant K as Kafka
  participant T as ticket
  participant DB as ticket-db
  K->>T: OrderPaid cho 5 vé
  T->>DB: BEGIN, insert vé 1 2 3
  Note over T: lỗi giữa vòng lặp
  T->>DB: ROLLBACK, không có vé nửa vời
  K->>T: retry cùng event
  T->>DB: BEGIN, insert 5 vé, UK order_id tier_id seq
  DB-->>T: COMMIT
  T->>K: TicketsIssued
  Note over T,DB: phát hành trong 1 transaction nên chạy lại là idempotent
```

### S10-D11 — #16 Retry storm và cách cắt vòng phản hồi
```mermaid
flowchart TB
  SLOW["Service chậm"] --> TO["Client timeout"]
  TO --> RT["Mọi client retry đồng loạt"]
  RT --> LOAD["RPS tăng đột ngột"]
  LOAD --> SLOW
  BO["Exponential backoff + jitter"] -.->|"làm lệch thời điểm retry"| RT
  CB["Circuit breaker"] -.->|"ngắt gọi service đang lỗi"| TO
  RL["429 Retry-After"] -.->|"bảo client chờ"| LOAD
  BH["Bulkhead và shed load"] -.->|"từ chối bớt để cứu hệ thống"| LOAD
```

### Các tình huống đã có sequence ở S04
- **#5** Order crash sau reserve → xem S04-D6.
- **#9** Webhook đến sau khi hold hết hạn → xem S04-D8.
- **#11** Ticket-service down sau khi thu tiền → xem S04-D9.
- **#10** Webhook giả mạo → S07-D7 và S07-D9 (HMAC + timestamp + dedupe).

## D. Giải thích sơ đồ
- Đọc mỗi chuỗi từ trái sang phải như một ca postmortem: *sự cố → nguyên nhân → làm sao biết → làm sao phòng → làm sao hồi phục → cái giá phải trả*.
- Mẫu chung xuyên suốt: **idempotency, timeout có giá trị rõ ràng, outbox, state machine từ chối chuyển trạng thái sai, thời gian DB làm chuẩn, và alert dựa trên tuổi/lag**.
- Cột Trade-off nhắc rằng mọi biện pháp đều có giá (thêm job nền, thêm storage, tx dài hơn, friction cho người thật).

## E. Những điều phải nhớ
- **Hai quyết định chưa chốt:** #2 waiting room fail-open hay fail-closed (ADR-009); #9 refund hay cứu order (ADR-010). Source không chọn hộ bạn.
- Source cũng để mở "rebuild queue từ đâu khi Redis chết".
- Mọi retry phải có cùng khóa idempotent (orderId, Idempotency-Key, eventId).
- Dùng **thời gian DB** (`now()`) cho expiry để tránh clock skew.
- **SOURCE CHƯA QUY ĐỊNH:** giá trị N giây ở #5, thời gian giữ Idempotency-Key, số lần retry/DLQ N ở #14, ngưỡng alert cho lag/outbox/DLQ, cơ chế "cứu order" ở #9, Recover và Trade-off của #15 và #17 (source ghi —).

## F. Liên kết
- Đầu vào: S04, S06, S07, S09. Đầu ra: S11 (chaos/duplicate/concurrency test), S12 (#16), S18 (bug lab), Phase 4–10.

## G. Checklist
- [x] 19 tình huống đủ 6 trường  - [x] 5 sequence bổ sung (#4, #6, #7, #12, #16)  - [x] tham chiếu S04 cho #5, #9, #11  - [x] hai quyết định mở
