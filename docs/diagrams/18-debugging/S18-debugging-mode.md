# S18 — Debugging Mode

> Nguồn: mục "18. DEBUGGING MODE — bug cố tình đưa vào".

## A. Mục tiêu
- Luyện quy trình debug có hệ thống bằng 12 bug cố tình đưa vào (nhánh `bug/xx`).
- Liên hệ: S10 (failure scenarios), S09 (công cụ quan sát), S12 (đo), Phase 10.

## B. Overview diagram

### S18-D1 — Quy trình debug
```mermaid
flowchart LR
  O["Observe"] --> R["Reproduce"] --> I["Investigate"] --> RC["Identify root cause"] --> F["Fix"] --> T["Test"] --> PR["Prevent regression"] --> PM["Postmortem 10 dòng"]
```

## C. Detailed diagrams

### S18-D2 — 12 bug và kỹ năng luyện
```mermaid
flowchart LR
  B1["1 Race condition inventory<br/>bán 101 trên 100 vé"] --> K1["concurrency test, DB lock, constraint"]
  B2["2 N+1 ở me/orders"] --> K2["log SQL, EntityGraph"]
  B3["3 Duplicate event<br/>vé phát hành 2 lần sau rebalance"] --> K3["inbox, unique constraint"]
  B4["4 Memory leak<br/>SSE emitter không remove"] --> K4["heap dump, MAT hoặc VisualVM"]
  B5["5 Slow query<br/>2 triệu dòng, thiếu index hoặc OFFSET sâu"] --> K5["EXPLAIN ANALYZE"]
  B6["6 Deadlock<br/>mua nhiều tier theo thứ tự ngược"] --> K6["pg_locks, lock ordering"]
  B7["7 Cache inconsistency<br/>còn vé hoặc giá cũ"] --> K7["invalidate theo event"]
  B8["8 Retry storm"] --> K8["backoff + jitter, CB"]
  B9["9 Consumer lag<br/>notification lag hàng chục nghìn event"] --> K9["partition, batch, profiling"]
  B10["10 Authentication bug<br/>refresh cũ vẫn dùng được hoặc IDOR check-in"] --> K10["security test, ownership check"]
  B11["11 Transactional self-invocation<br/>rollback không xảy ra"] --> K11["hiểu proxy AOP"]
  B12["12 Timezone bug<br/>mở bán lệch 7 tiếng"] --> K12["UTC everywhere, Instant hoặc OffsetDateTime"]
```

## D. Giải thích sơ đồ
- D1: mỗi bug phải đi hết 8 bước, đặc biệt **Prevent regression** (test chống tái phát) và postmortem ngắn.
- D2: mỗi bug nhắm đúng một kỹ năng gốc: bug #1 và #6 là cạnh tranh DB, #3 và #9 là Kafka, #4 là JVM, #10 là bảo mật, #11 và #12 là lỗi tinh vi của framework/thời gian.

## E. Những điều phải nhớ
- DoD Phase 10: mỗi bug có postmortem ngắn + test chống tái phát.
- **SOURCE CHƯA QUY ĐỊNH:** cách "cài" bug vào mã (nhánh `bug/xx` chỉ là tên), thứ tự làm bug, bug nào thuộc phase nào.

## F. Liên kết
- Đầu vào: S09, S10, S11. Đầu ra: Phase 10, `docs/learning/bug-lab/`, `postmortems/`.

## G. Checklist
- [x] 8 bước quy trình  - [x] 12 bug với triệu chứng và kỹ năng
