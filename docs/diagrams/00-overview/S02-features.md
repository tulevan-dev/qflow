# S02 — Features (MVP và Advanced)

> Nguồn: mục "2. FEATURES" (2.1 MVP, 2.2 Advanced).

## A. Mục tiêu
- Chốt phạm vi MVP chạy end-to-end và danh sách nâng cấp.
- Liên hệ: S16 (feature nào build ở phase nào), S22 (Feature prioritization), S23 (mở rộng).

## B. Overview diagram

### S02-D1 — MVP và Advanced
```mermaid
mindmap
  root((Features))
    MVP
      1 Đăng ký đăng nhập JWT access + refresh rotation
      2 Tạo sự kiện tier submit duyệt PUBLISHED
      3 List search filter phân trang Postgres full-text
      4 Checkout reserve TTL 10p thanh toán mock phát hành vé
      5 Chống oversell tuyệt đối có concurrent test
      6 Vé QR và check-in không trùng
      7 Notification in-app và email qua outbox
      8 Idempotency-Key cho tạo order và thanh toán
      9 Correlation ID xuyên gateway service Kafka
    Advanced
      Waiting room Redis ZSET SSE admission token
      Rate limiting gateway
      Refund và compensation
      Audit service append-only
      Real-time dashboard cho organizer
      Bot mitigation nhẹ
      Analytics read model CQRS-lite
      Waitlist khi sold-out
```

## C. Detailed diagrams

### S02-D2 — Feature → phase xây dựng
> **ĐỀ XUẤT BỔ SUNG:** source không có bảng "feature → phase" trực tiếp. Ánh xạ dưới đây suy ra từ nội dung Phase 0–8 và mục Expansion; mục chưa có phase rõ ràng gắn nhãn **SOURCE CHƯA QUY ĐỊNH**.
```mermaid
flowchart LR
  F9["Correlation ID"] --> P0["Phase 0"]
  F1["Auth JWT + refresh rotation"] --> P1["Phase 1"]
  F2["Workflow duyệt sự kiện"] --> P2["Phase 2"]
  F3["List search filter phân trang"] --> P2
  F5["Chống oversell"] --> P4["Phase 4"]
  F8["Idempotency-Key tạo order"] --> P5["Phase 5"]
  F4["Checkout end-to-end"] --> P6["Phase 6"]
  AF3["Refund và compensation"] --> P6
  F6["QR và check-in"] --> P7["Phase 7"]
  F7["Notification"] --> P7
  AF4["Audit service"] --> P7
  AF1["Waiting room"] --> P8["Phase 8"]
  AF2["Rate limiting"] --> P8
  AF5["Real-time dashboard"] --> V21["v2.1 Expansion"]
  AF7["Analytics read model"] --> V30["v3.0 Expansion"]
  AF6["Bot mitigation"] --> U1["SOURCE CHƯA QUY ĐỊNH phase"]
  AF8["Waitlist"] --> U2["SOURCE CHƯA QUY ĐỊNH phase"]
```

## D. Giải thích sơ đồ
- **D1** giữ nguyên 9 mục MVP và 8 mục Advanced của source.
- **D2** cho thấy phần lớn MVP xây dần qua Phase 0–7; Advanced tập trung ở Phase 8 và các version mở rộng. `Bot mitigation` và `Waitlist` source chỉ liệt kê feature (Waitlist có độ ưu tiên P2) mà không gán phase.

## E. Những điều phải nhớ
- MVP "chạy được end-to-end": đăng ký → duyệt sự kiện → mua → vé → check-in.
- Chống oversell là tiêu chí **tuyệt đối**, không phải "cố gắng".
- Real-time dashboard và analytics không thuộc MVP.

## F. Liên kết
- Đầu vào: S01. Đầu ra: S16, S22, S23.

## G. Checklist
- [x] 9 MVP  - [x] 8 Advanced  - [x] ánh xạ phase (đánh dấu đề xuất)
