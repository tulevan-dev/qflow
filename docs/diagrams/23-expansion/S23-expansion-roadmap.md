# S23 — Expansion Roadmap

> Nguồn: mục "23. EXPANSION ROADMAP (mỗi version = bài toán kỹ thuật mới)".

## A. Mục tiêu
- Mỗi version sau v1.0 phải tạo ra **bài toán kỹ thuật mới**, không chỉ thêm CRUD.
- Liên hệ: S16 (Phase 12), S02 (Advanced features), S22 (DO NOT BUILD YET).

## B. Overview diagram

### S23-D1 — Chuỗi version và bài toán mới
```mermaid
flowchart TB
  V10["v1.0<br/>Workflow + checkout saga + QR check-in<br/>Consistency, idempotency, saga"] --> V11["v1.1<br/>Notification đa kênh, email digest, preference<br/>Retry, template, rate limit gửi, dedupe"]
  V11 --> V20["v2.0<br/>Waiting room + fairness + rate limit nâng cao<br/>Thundering herd, Redis Lua, backpressure"]
  V20 --> V21["v2.1<br/>Real-time dashboard organizer, live check-in map<br/>SSE scale, fan-out, read model"]
  V21 --> V30["v3.0<br/>Analytics pipeline hourly và daily rollup, xuất CSV hoặc PDF bất đồng bộ<br/>Batch hoặc stream aggregation, file processing, job scheduling"]
  V30 --> V31["v3.1<br/>Seat map, giữ ghế theo từng ghế<br/>Locking mịn, bitmap hoặc range lock, hot-spot"]
  V31 --> V32["v3.2<br/>Dynamic pricing, early-bird<br/>Rule engine, nhất quán giá, cache"]
  V32 --> V40["v4.0<br/>Multi-tenant: workspace, plan, quota<br/>Tenant isolation, row-level security, rate limit theo tenant"]
  V40 --> V41["v4.1<br/>Debezium CDC thay outbox poller, schema registry<br/>CDC, schema evolution"]
  V41 --> V50["v5.0<br/>Kubernetes, autoscale, multi-AZ — chỉ khi có tải thật<br/>Orchestration, HA"]
```

## C. Detailed diagrams
Không có sơ đồ chi tiết riêng: S23-D1 đã chứa mọi version và bài toán kỹ thuật mới; kiến trúc đích của từng version chưa được source mô tả.

## D. Giải thích sơ đồ
- Mỗi nút gồm **tên version**, **thay đổi** và **bài toán kỹ thuật mới**.
- Một số version quay lại chủ đề đã học ở mức sâu hơn (v2.0 về waiting room nâng cao so với Phase 8; v4.1 nhắc lại ý "thay relay bằng Debezium" của ADR-005).

## E. Những điều phải nhớ
- Không thêm tính năng chỉ vì "có thể": mỗi version phải trả lời "bài toán kỹ thuật mới là gì".
- Mục **DO NOT BUILD YET** của S22 vẫn đúng với tới khi tới version tương ứng.
- **SOURCE CHƯA QUY ĐỊNH:** thời lượng và điều kiện kích hoạt từng version; Phase 12 chỉ nhắc "v1.1 → v4" nhưng bảng còn có v5.0.

## F. Liên kết
- Đầu vào: Phase 11 (v1.0.0). Đầu ra: Phase 12.

## G. Checklist
- [x] 10 version v1.0 → v5.0  - [x] bài toán kỹ thuật mới của từng version
