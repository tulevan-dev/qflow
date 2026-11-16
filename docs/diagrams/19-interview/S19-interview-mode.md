# S19 — Interview Mode

> Nguồn: mục "19. INTERVIEW MODE (khung)".

## A. Mục tiêu
- Sau mỗi major phase có bộ câu hỏi: 10 lý thuyết · 10 project · 5 debugging · 5 system design · 5 production.
- Trả lời được 10 câu "xương sống" bằng lý lẽ chứ không học thuộc.
- Liên hệ: S16 (interview của từng phase), S10 (tình huống), checklist cuối.

## B. Overview diagram

### S19-D1 — Cấu trúc bộ phỏng vấn
```mermaid
mindmap
  root((Interview Mode))
    Bộ sau mỗi phase
      10 lý thuyết
      10 liên quan project
      5 debugging
      5 system design
      5 tình huống production
    10 câu xương sống
      Service chết giữa transaction
      Kafka gửi event 2 lần
      2 user mua vé cuối cùng
      Redis chết
      DB chậm
      Request gửi 5 lần
      Vì sao không dùng distributed transaction
      Vì sao tách payment và không tách pricing
      Webhook đến sau khi hết hạn giữ chỗ
      Thiết kế hệ thống bán 10.000 vé trong 1 phút cho 100.000 người
```

## C. Detailed diagrams

### S19-D2 — Câu hỏi → khái niệm cần hiểu → mục liên quan
```mermaid
flowchart LR
  Q1["Service chết giữa transaction?"] --> C1["DB rollback; còn side-effect ngoài DB thì sao"] --> L1["S04, S06"]
  Q2["Kafka gửi event 2 lần?"] --> C2["Inbox idempotent; vì sao không tin exactly-once"] --> L2["S06, S10-7"]
  Q3["2 user mua vé cuối cùng cùng lúc?"] --> C3["atomic update, isolation level, constraint"] --> L3["S04-D11, S05"]
  Q4["Redis chết?"] --> C4["phản ứng khác nhau theo use case: queue, rate limit, cache"] --> L4["S10-2, Phase 8"]
  Q5["DB chậm?"] --> C5["đo, xác định, index hoặc query hoặc pool hoặc cache; không chỉ thêm RAM"] --> L5["S12"]
  Q6["Request gửi 5 lần?"] --> C6["Idempotency-Key"] --> L6["S08-D5, S10-6"]
  Q7["Vì sao không dùng distributed transaction?"] --> C7["blocking, coupling, không phải hệ nào cũng hỗ trợ; saga + compensation"] --> L7["S06-D8"]
  Q8["Vì sao tách payment, không tách pricing?"] --> C8["ranh giới ngoài, idempotent, cô lập dữ liệu nhạy cảm"] --> L8["S03-D3, D4"]
  Q9["Webhook đến sau khi hết hạn giữ chỗ?"] --> C9["state machine từ chối; refund hoặc cứu order, ADR-010"] --> L9["S04-D8"]
  Q10["Thiết kế bán 10.000 vé trong 1 phút cho 100.000 người?"] --> C10["waiting room + inventory + saga: đề system design ruột của project"] --> L10["S03, S04, Phase 8"]
```

## D. Giải thích sơ đồ
- D1: khung chung; câu "xương sống" là điểm tựa để kiểm tra hiểu thật.
- D2: mỗi câu trỏ về khái niệm gốc và các sơ đồ đã vẽ, để bạn tự trả lời rồi đối chiếu.

## E. Những điều phải nhớ
- Mình (mentor) sẽ tạo bộ câu hỏi đầy đủ **sau mỗi phase** khi bạn báo xong.
- **SOURCE CHƯA QUY ĐỊNH:** đáp án chuẩn từng câu (source chỉ gợi ý hướng), điểm đạt của "interview checkpoint".

## F. Liên kết
- Đầu vào: S04–S12. Đầu ra: cổng chuyển phase (S16-D3), checklist.

## G. Checklist
- [x] khung 5 loại câu hỏi  - [x] 10 câu xương sống
