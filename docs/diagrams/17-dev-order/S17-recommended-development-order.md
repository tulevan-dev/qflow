# S17 — Recommended Development Order

> Nguồn: mục "17. RECOMMENDED DEVELOPMENT ORDER".

## A. Mục tiêu
- Chốt thứ tự build chính xác (13 bước), quy tắc khóa phase và lý do Observability đầy đủ ở cuối.
- Liên hệ: S16 (roadmap), S15 (tiến hóa), S22.

## B. Overview diagram

### S17-D1 — 13 bước theo thứ tự và quy tắc khóa phase
```mermaid
flowchart TB
  s1["1 Phase 0: Repo, CI, compose, error model, correlation id, logging, ADR 1–5"] --> s2["2 Phase 1: identity + gateway — auth chuẩn trước, đừng vá sau"]
  s2 --> s3["3 Phase 2: catalog + workflow — state machine, ownership"]
  s3 --> s4["4 Phase 3: frontend nền — song song: login, list, organizer, moderator"]
  s4 --> s5["5 Phase 4: inventory — concurrency core, KHÔNG bỏ qua, KHÔNG rút gọn"]
  s5 --> s6["6 Phase 5: order sync + gRPC — dual-write, reconciler"]
  s6 --> s7["7 Phase 6: Kafka + outbox + payment + ticket — phase khó nhất"]
  s7 --> s8["8 Phase 7: notification + audit + SSE + check-in"]
  s8 --> s9["9 Phase 8: waiting room + rate limit"]
  s9 --> s10["10 Phase 9: observability + chaos"]
  s10 --> s11["11 Phase 10: debug lab + perf"]
  s11 --> s12["12 Phase 11: deploy + public"]
  s12 --> s13["13 Phase 12: expansion v1.1+"]
  LOCK["Khóa phase: chưa qua DoD + interview checkpoint của phase trước thì không sang phase sau"] -.-> s2
```

## C. Detailed diagrams

### S17-D2 — Vì sao Observability đầy đủ ở cuối
```mermaid
flowchart LR
  P0["Phase 0: logging JSON + correlation id"] --> P1["Phase 1: health + metrics cơ bản"]
  P1 --> WHY["Dashboard và tracing đầy đủ chỉ có ý nghĩa khi đã có hệ thống phân tán để quan sát"]
  WHY --> P9["Phase 9: tracing end-to-end, dashboard, alert, chaos"]
```

## D. Giải thích sơ đồ
- Thứ tự đi từ nền tảng bảo mật (auth) → domain có workflow → lõi tranh chấp (inventory) → checkout sync → bất đồng bộ → side-effect → tải đột biến → quan sát → debug → deploy → mở rộng.
- Phase 4 bị nhấn mạnh "KHÔNG bỏ qua / KHÔNG rút gọn" vì là nơi học concurrency thật.
- Observability được chia đôi: **nền tảng sớm** (log JSON, correlation id, health, metrics cơ bản) và **đầy đủ muộn** (Phase 9).

## E. Những điều phải nhớ
- Bước 4 (Phase 3) chạy song song, không phải tuần tự cứng.
- **SOURCE CHƯA QUY ĐỊNH:** nếu thứ tự thay đổi (ví dụ bật Kafka sớm ở Phase 4) thì cần ADR; source chưa nói ảnh hưởng đến thứ tự.

## F. Liên kết
- Đầu vào: S16. Đầu ra: kế hoạch thực thi từng phase (P00–P12).

## G. Checklist
- [x] 13 bước  - [x] quy tắc khóa phase  - [x] vì sao Observability ở cuối
