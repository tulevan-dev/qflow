# PHASE 12 — Advanced Architecture (tùy chọn, sau v1.0)

> Nguồn: mục "16. ROADMAP" → PHASE 12. Thời lượng ước lượng: tùy chọn. Ô ghi `SOURCE CHƯA QUY ĐỊNH` nghĩa là source không có trường đó cho phase này.

## A. Mục tiêu
- **Goal:** Mở rộng sau v1.0 theo mục 23 (v1.1 → v4 trong source Phase 12; bảng mục 23 còn có v5.0).
- Vị trí trong roadmap: xem S16 (D1–D3). Kiến trúc stage tương ứng: xem S15-D3.

## B. Overview diagram

### P12-D1 — Input → Goal → Output → DoD → Gate
> **ĐỀ XUẤT BỔ SUNG:** phần *Input* được suy ra từ thứ tự phase và nội dung; riêng DoD, Goal, Features lấy từ source.
```mermaid
flowchart LR
  IN["Input<br/>Phase 11: v1.0.0"] --> G["Goal<br/>Mở rộng sau v1.0 theo mục 23 (v1.1 → v4 trong source Phase 12; bảng mục 23 còn có v5.0)."]
  G --> F["Features<br/>Xem S23"]
  G --> A["Architecture / Technology<br/>SOURCE CHƯA QUY ĐỊNH"]
  F --> O["Output<br/>Các version mở rộng (S23)"]
  A --> O
  O --> D["Definition of Done<br/>SOURCE CHƯA QUY ĐỊNH"]
  D --> GT{"Gate: DoD + interview checkpoint"}
  GT -->|"đạt"| N["(hết roadmap)"]
  GT -->|"chưa đạt"| G
```

## C. Chi tiết phase (đúng các trường của source)
| Trường | Nội dung |
|---|---|
| Goal | Mở rộng sau v1.0 theo mục 23 (v1.1 → v4 trong source Phase 12; bảng mục 23 còn có v5.0). |
| Features | Xem S23 |
| Architecture / Technology | SOURCE CHƯA QUY ĐỊNH |
| Concepts learned | SOURCE CHƯA QUY ĐỊNH |
| Coding tasks | SOURCE CHƯA QUY ĐỊNH |
| Expected difficulty | SOURCE CHƯA QUY ĐỊNH |
| Optimization opportunity | SOURCE CHƯA QUY ĐỊNH |

**Definition of Done**
- SOURCE CHƯA QUY ĐỊNH

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
- Đầu vào: Phase 11: v1.0.0. Đầu ra: Các version mở rộng (S23). Phase kế tiếp: (hết roadmap).

## G. Checklist
- [x] Goal  - [x] Features  - [x] Architecture/Technology  - [x] Concepts  - [x] Coding tasks  - [x] Difficulty  - [x] DoD  - [x] Mistakes  - [x] Optimization  - [x] Interview (ô thiếu đã ghi rõ)
