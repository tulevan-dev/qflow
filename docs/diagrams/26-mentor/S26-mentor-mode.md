# S26 — Vận hành kiểu Mentor

> Nguồn: mục "26. VẬN HÀNH KIỂU MENTOR (cách mình sẽ hỗ trợ bạn)".

## A. Mục tiêu
- Quy định cách học và cách nhận phản hồi: bạn tự làm, mentor không code hộ; phản hồi theo khung 8 bước.
- Liên hệ: S19 (interview), S18 (debug), S16 (phase).

## B. Overview diagram

### S26-D1 — Vòng học cho mỗi phase
```mermaid
flowchart LR
  P["Problem"] --> RQ["Requirement"] --> H["Gợi ý câu hỏi"] --> TRY["Bạn tự triển khai"] --> SOL["Solution + Trade-off<br/>sau khi bạn thử"]
  TRY --> SEND["Gửi code hoặc kiến trúc để review"]
  SEND --> FB["Phản hồi theo khung 8 bước"]
  FB --> TRY
```

## C. Detailed diagrams

### S26-D2 — Khung phản hồi khi bạn gửi code hoặc kiến trúc
```mermaid
flowchart TB
  A["1 Bug"] --> B["2 Root cause"] --> C["3 Why it happens"] --> D["4 Correct solution"] --> E["5 Alternative"] --> F["6 Trade-off"] --> G["7 Production consideration"] --> H["8 Kiến thức phỏng vấn liên quan"]
  W1["Cảnh báo thẳng nếu code chạy nhưng kiến trúc sai"] -.-> D
  W2["Cảnh báo nếu code tốt nhưng đang overengineering"] -.-> F
```

## D. Giải thích sơ đồ
- D1: mentor đưa vấn đề và yêu cầu trước, để bạn thử, sau đó mới đưa lời giải và trade-off; không biến thành hỏi đáp từng câu liên tục.
- D2: phản hồi không chỉ sửa code mà giải thích gốc rễ, phương án thay thế, đánh đổi và kiến thức phỏng vấn.

## E. Những điều phải nhớ
- Mục tiêu là hiểu kiến trúc chứ không thuộc kiến trúc.
- **SOURCE CHƯA QUY ĐỊNH:** tần suất review, cách gửi code (repo hay đoạn trích).

## F. Liên kết
- Đầu vào: S16, S19. Đầu ra: Phase 0 → bắt đầu thực thi.

## G. Checklist
- [x] vòng học  - [x] khung 8 bước  - [x] cảnh báo overengineering
