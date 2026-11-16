# PHASE 9 — Observability + Resilience

> Nguồn: mục "16. ROADMAP" → PHASE 9. Thời lượng ước lượng: ≈2 tuần. Ô ghi `SOURCE CHƯA QUY ĐỊNH` nghĩa là source không có trường đó cho phase này.

## A. Mục tiêu
- **Goal:** Nhìn vào hệ thống trả lời được 5 câu hỏi ở mục 9.
- Vị trí trong roadmap: xem S16 (D1–D3). Kiến trúc stage tương ứng: xem S15-D3.

## B. Overview diagram

### P9-D1 — Input → Goal → Output → DoD → Gate
> **ĐỀ XUẤT BỔ SUNG:** phần *Input* được suy ra từ thứ tự phase và nội dung; riêng DoD, Goal, Features lấy từ source.
```mermaid
flowchart LR
  IN["Input<br/>Phase 8: hệ thống đủ thành phần"] --> G["Goal<br/>Nhìn vào hệ thống trả lời được 5 câu hỏi ở mục 9."]
  G --> F["Features<br/>Tracing, dashboard, alert, health, chaos"]
  G --> A["Architecture / Technology<br/>OpenTelemetry + Prometheus + Grafana + Jaeger"]
  F --> O["Output<br/>Dashboard + alert + trace end-to-end + chaos script"]
  A --> O
  O --> D["Definition of Done<br/>Từ 1 correlationId truy ra toàn bộ hành trình; Kill 1 service → alert bắn trong ngưỡng"]
  D --> GT{"Gate: DoD + interview checkpoint"}
  GT -->|"đạt"| N["Phase 10"]
  GT -->|"chưa đạt"| G
```

## C. Chi tiết phase (đúng các trường của source)
| Trường | Nội dung |
|---|---|
| Goal | Nhìn vào hệ thống trả lời được 5 câu hỏi ở mục 9. |
| Features | Tracing end-to-end qua HTTP + gRPC + Kafka; dashboard; alert (consumer lag, outbox backlog, DLQ > 0, order kẹt); health/readiness chuẩn; chaos script |
| Architecture / Technology | SOURCE CHƯA QUY ĐỊNH |
| Concepts learned | Ba trụ cột observability; trace qua Kafka (nội dung chi tiết xem S09). |
| Coding tasks | SOURCE CHƯA QUY ĐỊNH |
| Expected difficulty | SOURCE CHƯA QUY ĐỊNH |
| Optimization opportunity | SOURCE CHƯA QUY ĐỊNH |

**Definition of Done**
- [ ] Từ 1 correlationId truy ra toàn bộ hành trình
- [ ] Kill 1 service → alert bắn trong ngưỡng

**Common mistakes**
- Log PII/token
- Cardinality label bùng nổ (userId làm metric label)
- Alert quá nhiều

**Interview (câu mẫu trong source)**
- 3 trụ cột observability?
- Latency p99 vs average?
- Làm sao trace qua Kafka?

## D. Giải thích sơ đồ
- Sơ đồ đọc từ trái sang phải: đầu vào từ phase trước → mục tiêu → hai nhánh song song (tính năng, kiến trúc/công nghệ) → sản phẩm đầu ra → DoD → **cổng chuyển phase**.
- Cổng chuyển phase theo source: chưa qua **DoD + interview checkpoint** của phase này thì không sang phase sau. "Interview checkpoint" là bộ câu hỏi (10 lý thuyết, 10 project, 5 debugging, 5 system design, 5 production) mà source nói sẽ tạo sau mỗi phase; ở đây chỉ có câu mẫu.

## E. Những điều phải nhớ
- Mỗi lỗi trong "Common mistakes" là lỗi thật mà phase này dễ mắc; dùng làm checklist review.
- Tiêu chí DoD phải **kiểm chứng được** bằng test, số đo hoặc demo.
- **SOURCE CHƯA QUY ĐỊNH:** mọi ô `SOURCE CHƯA QUY ĐỊNH` ở bảng trên.

## F. Liên kết
- Đầu vào: Phase 8: hệ thống đủ thành phần. Đầu ra: Dashboard + alert + trace end-to-end + chaos script. Phase kế tiếp: Phase 10.

## G. Checklist
- [x] Goal  - [x] Features  - [x] Architecture/Technology  - [x] Concepts  - [x] Coding tasks  - [x] Difficulty  - [x] DoD  - [x] Mistakes  - [x] Optimization  - [x] Interview (ô thiếu đã ghi rõ)
