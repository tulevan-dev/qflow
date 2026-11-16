# PHASE 7 — Notification, Audit, Real-time (SSE)

> Nguồn: mục "16. ROADMAP" → PHASE 7. Thời lượng ước lượng: ≈2 tuần. Ô ghi `SOURCE CHƯA QUY ĐỊNH` nghĩa là source không có trường đó cho phase này.

## A. Mục tiêu
- **Goal:** Side-effect và read model bằng choreography.
- Vị trí trong roadmap: xem S16 (D1–D3). Kiến trúc stage tương ứng: xem S15-D3.

## B. Overview diagram

### P7-D1 — Input → Goal → Output → DoD → Gate
> **ĐỀ XUẤT BỔ SUNG:** phần *Input* được suy ra từ thứ tự phase và nội dung; riêng DoD, Goal, Features lấy từ source.
```mermaid
flowchart LR
  IN["Input<br/>Phase 6: event stream"] --> G["Goal<br/>Side-effect và read model bằng choreography."]
  G --> F["Features<br/>Notification, SSE, audit, QR + check-in"]
  G --> A["Architecture / Technology<br/>notification + audit + SSE (chi tiết: SOURCE CHƯA QUY ĐỊNH)"]
  F --> O["Output<br/>notification, audit, SSE, QR + check-in"]
  A --> O
  O --> D["Definition of Done<br/>Tắt notification-service → checkout vẫn chạy; Bật lại → thông báo được gửi bù, không gửi trùng"]
  D --> GT{"Gate: DoD + interview checkpoint"}
  GT -->|"đạt"| N["Phase 8"]
  GT -->|"chưa đạt"| G
```

## C. Chi tiết phase (đúng các trường của source)
| Trường | Nội dung |
|---|---|
| Goal | Side-effect và read model bằng choreography. |
| Features | In-app + email (mock SMTP/Mailpit) với retry; SSE order status/notification; audit log append-only; ticket QR + check-in (idempotent, offline-friendly ở mức xử lý trùng) |
| Architecture / Technology | SOURCE CHƯA QUY ĐỊNH |
| Concepts learned | SSE vs WebSocket, backpressure, reconnect/Last-Event-ID, local read model, eventual consistency UX. |
| Coding tasks | SOURCE CHƯA QUY ĐỊNH |
| Expected difficulty | SOURCE CHƯA QUY ĐỊNH |
| Optimization opportunity | SOURCE CHƯA QUY ĐỊNH |

**Definition of Done**
- [ ] Tắt notification-service → checkout vẫn chạy
- [ ] Bật lại → thông báo được gửi bù, không gửi trùng

**Common mistakes**
- Gửi email trong luồng chính
- SSE không heartbeat
- Rò quyền (user A nghe được stream của B)

**Interview (câu mẫu trong source)**
- SSE vs WebSocket vs polling?
- Làm sao UI hiển thị trạng thái khi dữ liệu eventual?
- Tránh gửi email trùng thế nào?

## D. Giải thích sơ đồ
- Sơ đồ đọc từ trái sang phải: đầu vào từ phase trước → mục tiêu → hai nhánh song song (tính năng, kiến trúc/công nghệ) → sản phẩm đầu ra → DoD → **cổng chuyển phase**.
- Cổng chuyển phase theo source: chưa qua **DoD + interview checkpoint** của phase này thì không sang phase sau. "Interview checkpoint" là bộ câu hỏi (10 lý thuyết, 10 project, 5 debugging, 5 system design, 5 production) mà source nói sẽ tạo sau mỗi phase; ở đây chỉ có câu mẫu.

## E. Những điều phải nhớ
- Mỗi lỗi trong "Common mistakes" là lỗi thật mà phase này dễ mắc; dùng làm checklist review.
- Tiêu chí DoD phải **kiểm chứng được** bằng test, số đo hoặc demo.
- **SOURCE CHƯA QUY ĐỊNH:** mọi ô `SOURCE CHƯA QUY ĐỊNH` ở bảng trên.

## F. Liên kết
- Đầu vào: Phase 6: event stream. Đầu ra: notification, audit, SSE, QR + check-in. Phase kế tiếp: Phase 8.

## G. Checklist
- [x] Goal  - [x] Features  - [x] Architecture/Technology  - [x] Concepts  - [x] Coding tasks  - [x] Difficulty  - [x] DoD  - [x] Mistakes  - [x] Optimization  - [x] Interview (ô thiếu đã ghi rõ)
