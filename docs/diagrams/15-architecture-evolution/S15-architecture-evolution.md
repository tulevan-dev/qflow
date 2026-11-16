# S15 — Kiến trúc tiến hóa (microservices ngay từ đầu, nhưng không dựng tất cả cùng lúc)

> Nguồn: mục "15. KIẾN TRÚC TIẾN HÓA".

## A. Mục tiêu
- Giải thích vì sao kiến trúc thay đổi qua từng stage A→F: vấn đề nào kiến trúc cũ chưa giải quyết tốt và kiến trúc mới giải quyết gì.
- Liên hệ: S03 (kiến trúc đích), S16 (phase), S22 (không thêm service thứ 11).

## B. Overview diagram

### S15-D1 — Sáu stage (Walking Skeleton → Resilience)
```mermaid
flowchart LR
  A["Stage A Skeleton<br/>gateway + identity + catalog<br/>REST thuần, DB riêng"] --> B["Stage B + Checkout<br/>inventory + order sync<br/>gặp: consistency, contention, dual-write"]
  B --> C["Stage C + Async<br/>Kafka + Outbox + payment + ticket<br/>gặp: duplicate, ordering, saga"]
  C --> D["Stage D + Edge<br/>notification + audit + SSE<br/>gặp: eventual consistency, read model"]
  D --> E["Stage E + Scale<br/>waiting-room + Redis + rate limit<br/>gặp: thundering herd, fairness"]
  E --> F["Stage F + Resilience và Perf<br/>CB, DLQ replay, chaos, tuning"]
```

## C. Detailed diagrams

### S15-D2 — Vì sao phải đổi ở mỗi bước
```mermaid
flowchart LR
  subgraph AB["A → B"]
    direction LR
    a1["Cần mua vé"] --> a2["Cũ: chưa có tranh chấp tài nguyên"] --> a3["Mới: inventory độc lập, atomic update, hold TTL"]
  end
  subgraph BC["B → C"]
    direction LR
    b1["Order gọi sync mọi thứ"] --> b2["Cũ: 1 service chậm là cả luồng chết, dual-write mất đồng bộ khi crash"] --> b3["Mới: Event + Outbox + Saga"]
  end
  subgraph CD["C → D"]
    direction LR
    c1["Notification và audit gắn vào luồng chính"] --> c2["Cũ: side-effect nằm trong critical path, dễ lỗi lây"] --> c3["Mới: choreography consumers"]
  end
  subgraph DE["D → E"]
    direction LR
    d1["Lúc mở bán traffic dồn"] --> d2["Cũ: inventory và DB nghẽn, không công bằng, backpressure chỉ bằng rate limit là thô"] --> d3["Mới: waiting room điều tiết tốc độ vào"]
  end
  subgraph EF["E → F"]
    direction LR
    e1["Đã có hệ thống"] --> e2["Cũ: chưa biết nó chịu lỗi thế nào, tin vào happy path"] --> e3["Mới: chaos + metrics + tuning"]
  end
```

### S15-D3 — Stage ↔ Phase
> **ĐỀ XUẤT BỔ SUNG:** source không có bảng stage ↔ phase; ánh xạ suy ra từ nội dung từng phase.
```mermaid
flowchart LR
  SA["Stage A"] --> PA["Phase 0 đến 2 và 3"]
  SB["Stage B"] --> PB["Phase 4 và 5"]
  SC["Stage C"] --> PC["Phase 6"]
  SD["Stage D"] --> PD["Phase 7"]
  SE["Stage E"] --> PE["Phase 8"]
  SF["Stage F"] --> PF["Phase 9 và 10"]
```

## D. Giải thích sơ đồ
- Mỗi stage thêm **một loại vấn đề phân tán mới**: tranh chấp (B), dual-write và bất đồng bộ (C), eventual consistency (D), tải đột biến (E), khả năng chịu lỗi (F).
- Nguyên tắc: không dựng 10 service rồi mới chạy; thêm dần và chỉ thêm khi có vấn đề cụ thể.

## E. Những điều phải nhớ
- Mỗi lần đổi kiến trúc phải trả lời được: vì sao cần đổi, vấn đề của cũ, cái mới giải quyết gì (viết thành ADR, S20).
- Stage A→B đã tồn tại "microservices từ đầu" nhưng với số service nhỏ.
- **SOURCE CHƯA QUY ĐỊNH:** ánh xạ stage ↔ phase chính thức; thời điểm bật Kafka (Phase 4 nói có thể bật sớm hoặc chờ Phase 6).

## F. Liên kết
- Đầu vào: S03. Đầu ra: S16 (thứ tự phase), S17, S20 (ADR).

## G. Checklist
- [x] 6 stage A–F  - [x] bảng "vì sao đổi" (5 bước)  - [x] ánh xạ stage ↔ phase (đề xuất)
