# PHASE 10 — Debugging Lab + Performance Tuning

> Nguồn: mục "16. ROADMAP" → PHASE 10. Thời lượng ước lượng: ≈2–3 tuần. Ô ghi `SOURCE CHƯA QUY ĐỊNH` nghĩa là source không có trường đó cho phase này.

## A. Mục tiêu
- **Goal:** Chuyển từ "biết làm" sang "biết sửa".
- Vị trí trong roadmap: xem S16 (D1–D3). Kiến trúc stage tương ứng: xem S15-D3.

## B. Overview diagram

### P10-D1 — Input → Goal → Output → DoD → Gate
> **ĐỀ XUẤT BỔ SUNG:** phần *Input* được suy ra từ thứ tự phase và nội dung; riêng DoD, Goal, Features lấy từ source.
```mermaid
flowchart LR
  IN["Input<br/>Phase 9: observability để điều tra"] --> G["Goal<br/>Chuyển từ 'biết làm' sang 'biết sửa'."]
  G --> F["Features<br/>12 bug lab, perf journal ≥ 4 bài"]
  G --> A["Architecture / Technology<br/>SOURCE CHƯA QUY ĐỊNH"]
  F --> O["Output<br/>12 bug có postmortem + perf journal ≥ 4 bài"]
  A --> O
  O --> D["Definition of Done<br/>Mỗi bug có postmortem ngắn + test chống tái phát"]
  D --> GT{"Gate: DoD + interview checkpoint"}
  GT -->|"đạt"| N["Phase 11"]
  GT -->|"chưa đạt"| G
```

## C. Chi tiết phase (đúng các trường của source)
| Trường | Nội dung |
|---|---|
| Goal | Chuyển từ "biết làm" sang "biết sửa". |
| Features | Làm lần lượt các bug ở mục 18; Hoàn thành perf journal (baseline → optimize → đo lại) cho ≥ 4 bài toán ở mục 12 |
| Architecture / Technology | SOURCE CHƯA QUY ĐỊNH |
| Concepts learned | SOURCE CHƯA QUY ĐỊNH |
| Coding tasks | SOURCE CHƯA QUY ĐỊNH |
| Expected difficulty | SOURCE CHƯA QUY ĐỊNH |
| Optimization opportunity | SOURCE CHƯA QUY ĐỊNH |

**Definition of Done**
- [ ] Mỗi bug có postmortem ngắn + test chống tái phát

**Common mistakes**
- SOURCE CHƯA QUY ĐỊNH

**Interview (câu mẫu trong source)**
- SOURCE CHƯA QUY ĐỊNH

## D. Giải thích sơ đồ
- Sơ đồ đọc từ trái sang phải: đầu vào từ phase trước → mục tiêu → hai nhánh song song (tính năng, kiến trúc/công nghệ) → sản phẩm đầu ra → DoD → **cổng chuyển phase**.
- Cổng chuyển phase theo source: chưa qua **DoD + interview checkpoint** của phase này thì không sang phase sau. "Interview checkpoint" là bộ câu hỏi (10 lý thuyết, 10 project, 5 debugging, 5 system design, 5 production) mà source nói sẽ tạo sau mỗi phase; ở đây chỉ có câu mẫu.

## E. Những điều phải nhớ
- Mỗi lỗi trong "Common mistakes" là lỗi thật mà phase này dễ mắc; dùng làm checklist review.
- Tiêu chí DoD phải **kiểm chứng được** bằng test, số đo hoặc demo.
- **SOURCE CHƯA QUY ĐỊNH:** mọi ô `SOURCE CHƯA QUY ĐỊNH` ở bảng trên.

## F. Liên kết
- Đầu vào: Phase 9: observability để điều tra. Đầu ra: 12 bug có postmortem + perf journal ≥ 4 bài. Phase kế tiếp: Phase 11.

## G. Checklist
- [x] Goal  - [x] Features  - [x] Architecture/Technology  - [x] Concepts  - [x] Coding tasks  - [x] Difficulty  - [x] DoD  - [x] Mistakes  - [x] Optimization  - [x] Interview (ô thiếu đã ghi rõ)
