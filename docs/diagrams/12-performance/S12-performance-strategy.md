# S12 — Performance Strategy

> Nguồn: mục "12. PERFORMANCE STRATEGY" (quy trình đo, bảng 7 bài toán, benchmark methodology).

## A. Mục tiêu
- Tối ưu có phương pháp: đo trước, xác định bottleneck, tối ưu một thứ, đo lại, ghi nhật ký.
- Không bịa số: mọi benchmark do bạn đo trên máy bạn.
- Liên hệ: S09 (công cụ đo), S10 (#13, #16), S11 (load test), Phase 10.

## B. Overview diagram

### S12-D1 — Vòng tối ưu bắt buộc
```mermaid
flowchart LR
  B["Baseline"] --> M["Measure"]
  M --> I["Identify bottleneck"]
  I --> O["Optimize chỉ 1 thay đổi"]
  O --> M2["Measure again"]
  M2 --> J["Ghi vào docs/learning/perf-journal.md"]
  J -->|"vẫn chưa đạt"| M
  X["Tối ưu mù khi chưa có baseline"] -.->|"KHÔNG ĐƯỢC"| O
```

## C. Detailed diagrams

### S12-D2 — Bảy bài toán: tạo → đo → tối ưu
```mermaid
flowchart LR
  subgraph P1["Hot row contention trên tier_stock"]
    direction LR
    a1["Tạo: 5.000 VU mua 1 tier"] --> b1["Đo: k6, pg_stat_activity, lock wait"] --> c1["Tối ưu: atomic UPDATE, Redis Lua pre-check, sharded counter"]
  end
  subgraph P2["N+1 ở danh sách order/event"]
    direction LR
    a2["Tạo: bật hibernate.generate_statistics, log SQL"] --> b2["Đo: log, test đếm query"] --> c2["Tối ưu: JOIN FETCH, EntityGraph, DTO projection"]
  end
  subgraph P3["Query list chậm"]
    direction LR
    a3["Tạo: seed 1 đến 5 triệu dòng"] --> b3["Đo: EXPLAIN ANALYZE BUFFERS"] --> c3["Tối ưu: composite hoặc partial index, keyset pagination thay OFFSET"]
  end
  subgraph P4["Cache miss storm khi event hot"]
    direction LR
    a4["Tạo: xóa cache + spike"] --> b4["Đo: hit ratio"] --> c4["Tối ưu: cache-aside + TTL jitter + request coalescing"]
  end
  subgraph P5["Consumer lag"]
    direction LR
    a5["Tạo: produce 100k event"] --> b5["Đo: lag metric"] --> c5["Tối ưu: batch listener, partition, tối ưu idempotency write"]
  end
  subgraph P6["Hikari cạn"]
    direction LR
    a6["Tạo: giảm pool, tăng tải"] --> b6["Đo: hikaricp_connections_pending"] --> c6["Tối ưu: giảm thời gian giữ connection, không gọi mạng trong transaction"]
  end
  subgraph P7["Waiting room scale"]
    direction LR
    a7["Tạo: 20k kết nối SSE"] --> b7["Đo: số connection, memory"] --> c7["Tối ưu: virtual threads ADVANCED, tune Tomcat hoặc Netty, heartbeat"]
  end
```

### S12-D3 — Benchmark methodology
```mermaid
flowchart TB
  A["Cố định dataset bằng seed script"] --> B["Warm-up 1 đến 2 phút"]
  B --> C["Chạy ít nhất 3 lần"]
  C --> D["Lấy median"]
  D --> E["Ghi p50 p95 p99, error rate, CPU và DB metrics"]
  E --> F["Ghi cấu hình CPU, RAM, pool, dataset"]
  F --> G["Chỉ đổi 1 biến mỗi lần"]
  G --> A
```

### S12-D4 — Thang tối ưu cho hot row inventory
```mermaid
flowchart LR
  L1["Atomic UPDATE WHERE available >= q<br/>chọn trước"] -->|"đo thấy PG là bottleneck"| L2["Redis Lua pre-check"]
  L2 -->|"vẫn nghẽn hot row"| L3["Chia stock thành N bucket<br/>sharded counter"]
  L1 -.->|"Redis KHÔNG là source of truth ở v1"| PG[("PostgreSQL")]
```

## D. Giải thích sơ đồ
- **D1** là kỷ luật cốt lõi: mỗi lần chỉ một thay đổi để biết thay đổi nào có tác dụng.
- **D2** mỗi bài toán có đủ ba phần: cách **tạo** vấn đề, cách **đo**, và hướng **tối ưu chỉ sau khi đo**.
- **D3** giúp kết quả lặp lại được và so sánh công bằng giữa các lần.
- **D4** mỗi nấc chỉ lên khi nấc trước đã được đo là chưa đủ.

## E. Những điều phải nhớ
- Không bịa benchmark; chỉ ghi số bạn đo cùng cấu hình môi trường.
- Redis Lua và sharded counter là NICE TO HAVE, không làm trước khi đo.
- Không gọi mạng trong transaction (liên quan Hikari cạn).
- **SOURCE CHƯA QUY ĐỊNH:** ngưỡng mục tiêu (p95, RPS), cấu hình máy/dataset chuẩn, số VU cho từng kịch bản ngoài các ví dụ trong bảng.

## F. Liên kết
- Đầu vào: S09, S10, S11. Đầu ra: Phase 8 (waiting room load), Phase 10 (perf journal ≥ 4 bài), checklist Performance.

## G. Checklist
- [x] vòng đo  - [x] 7 bài toán  - [x] methodology  - [x] cảnh báo không bịa số
