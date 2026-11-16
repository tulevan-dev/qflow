# S20 — Workflow, Docs, ADR (20.1–20.3)

> Nguồn: mục "20. WORKFLOW, DOCS, ADR" (20.1 Git workflow, 20.2 Cấu trúc docs, 20.3 ADR).

## A. Mục tiêu
- Làm việc như team thật: quy trình từ yêu cầu đến deploy, checklist PR/review, cấu trúc tài liệu và 12 ADR cần viết.
- Liên hệ: S15 (mỗi lần đổi kiến trúc cần ADR), S21 (CI/CD), S25 (portfolio).

## B. Overview diagram

### S20-D1 — Quy trình phát triển
```mermaid
flowchart LR
  R["Requirement"] --> D["Design: ADR hoặc sơ đồ"] --> I["Issue"] --> B["Branch"] --> C["Code"] --> T["Test"] --> PR["Pull Request"] --> RV["Review: tự review bằng checklist"] --> CI["CI"] --> M["Merge"] --> DP["Deploy"]
```

## C. Detailed diagrams

### S20-D2 — Trunk-based nhẹ, branch ngắn, tag vX.Y.Z
```mermaid
gitGraph
  commit id: "init"
  branch feat/identity-login
  checkout feat/identity-login
  commit id: "feat(identity): login"
  checkout main
  merge feat/identity-login
  branch fix/inventory-negative-stock
  checkout fix/inventory-negative-stock
  commit id: "fix(inventory): prevent negative stock"
  checkout main
  merge fix/inventory-negative-stock tag: "v1.0.0"
```
Hình chỉ minh họa quy ước: `main` luôn deploy được; branch ngắn `feat/…`, `fix/…`, `chore/…`, `docs/…`; tag `vX.Y.Z`.

### S20-D3 — Cấu trúc docs
```mermaid
mindmap
  root((README.md và docs))
    architecture
      c4 context và container
      sequence diagram checkout saga
      event catalog
    api
      OpenAPI export
      error codes
      idempotency guide
    database
      ERD từng service
      giải thích vì sao
    decisions
      ADR-0001 và các ADR
    deployment
      compose
      VPS
      CI/CD
      backup và restore
      rollback
    troubleshooting
      runbook order kẹt PAID
      runbook consumer lag
      runbook DLQ có message
    learning
      perf-journal.md
      postmortems
      interview-notes
      bug-lab
```

### S20-D4 — 12 ADR cần viết
```mermaid
flowchart LR
  subgraph ARCH["Kiến trúc"]
    A1["001 Microservices ngay từ đầu, chấp nhận chi phí vận hành"]
    A2["002 Database-per-service, cấm cross-DB"]
    A3["004 Orchestrated saga cho checkout, choreography cho side-effect"]
    A4["005 Transactional Outbox, không dual-write hay 2PC"]
    A5["007 SSE thay WebSocket"]
    A6["008 gRPC chỉ cho order → inventory"]
    A7["012 Không Eureka, Config Server, K8s"]
  end
  subgraph DATA["Dữ liệu và đồng thời"]
    D1["003 PostgreSQL là source of truth tồn kho, Redis chỉ tăng tốc"]
    D2["006 Atomic UPDATE vs optimistic vs pessimistic, kèm số đo"]
  end
  subgraph DEC["Quyết định mở"]
    O1["009 Redis chết: waiting room fail-closed hay fail-open"]
    O2["010 Thanh toán đến muộn: refund hay cứu order"]
  end
  subgraph SEC["Bảo mật"]
    S1["011 Refresh token rotation + reuse detection"]
  end
```

### S20-D5 — Khung ADR và ví dụ ADR-005
```mermaid
flowchart TB
  T["Khung ADR"] --> C["Context"] --> De["Decision"] --> Al["Alternatives"] --> Tr["Trade-offs"] --> Co["Consequences"]
  subgraph EX["Ví dụ ADR-005 Outbox"]
    c1["Context: order cần ghi DB và phát event, crash giữa 2 việc thì mất event hoặc có event ma"]
    d1["Decision: ghi event vào outbox cùng transaction, relay đẩy Kafka"]
    a1["Alternatives: dual-write, 2PC hoặc XA, CDC Debezium"]
    t1["Trade-offs: thêm bảng và job, trễ vài trăm ms, at-least-once nên consumer phải idempotent"]
    q1["Consequences: mọi consumer có processed_events, cần metric outbox backlog, sau này thay relay bằng Debezium không đổi domain code"]
  end
  C -.-> c1
  De -.-> d1
  Al -.-> a1
  Tr -.-> t1
  Co -.-> q1
```

## D. Giải thích sơ đồ
- **D1–D2:** quy trình giống team thật nhưng gọn: trunk-based, branch ngắn, review bằng checklist tự làm, tag cho phiên bản.
- **D3:** mỗi thư mục docs trả lời một loại câu hỏi (kiến trúc, API, dữ liệu, quyết định, vận hành, sự cố, học tập).
- **D4–D5:** ADR giúp bạn *hiểu* kiến trúc thay vì *thuộc* kiến trúc: mỗi quyết định có bối cảnh, lựa chọn thay thế và cái giá.

## E. Những điều phải nhớ
- **Commit convention:** Conventional Commits, ví dụ `feat(order): add idempotency key`, `fix(inventory): prevent negative stock`.
- **Issue template:** Context · Acceptance Criteria · Out of scope · Technical notes · Test plan.
- **PR checklist:** có test (unit + integration nếu đụng DB/Kafka) · migration an toàn (backward compatible) · idempotent? · transaction boundary đúng? · có log/metric? · không lộ PII/secret · doc/ADR cập nhật · đã chạy lại concurrency test nếu đụng inventory/order.
- **Code review checklist (tự review):** ownership check? N+1? gọi mạng trong transaction? retry có idempotent? timeout đã đặt? lỗi có mã/ý nghĩa rõ? có abstraction thừa?
- **Definition of Done mỗi task:** chạy local bằng compose · test pass trong CI · không tăng warning · doc/ADR cập nhật · có metric/log phù hợp · demo được bằng 1 kịch bản.
- **SOURCE CHƯA QUY ĐỊNH:** công cụ issue/PR (GitHub là mặc định hợp lý nhưng source không nêu), quy tắc bảo vệ nhánh `main`, đánh số ADR chính xác (ADR-0001 hay ADR-001: source dùng cả hai), thời điểm viết từng ADR ngoài ADR 1–5 ở Phase 0 và ADR-006 ở Phase 4.
- Gitgraph ở D2 là hình minh họa quy ước, không phải lịch sử thật.

## F. Liên kết
- Đầu vào: S15, S16. Đầu ra: mọi phase (DoD), S21 (CI), S25 (README).

## G. Checklist
- [x] 20.1 workflow, branch, commit, issue, PR, review, DoD  - [x] 20.2 cây docs  - [x] 20.3 12 ADR + khung + ví dụ ADR-005
