# S16 — Roadmap (tổng quan, dependency, gate)

> Nguồn: mục "16. ROADMAP (phase-by-phase)" và mục "17. Recommended Development Order". Chi tiết từng phase nằm ở `P00` … `P12` cùng thư mục.

## A. Mục tiêu
- Giữ nguyên thứ tự 13 phase (Phase 0–12), thời lượng ước lượng, quan hệ phụ thuộc và **điều kiện chuyển phase**.
- Mỗi phase có file riêng với đủ trường: Goal, Features, Architecture/Technology, Concepts, Coding tasks, Difficulty, DoD, Mistakes, Optimization, Interview.
- Liên hệ: S15 (kiến trúc tiến hóa), S17 (thứ tự build), S18–S19 (debug/interview), checklist.

## B. Overview diagram

### S16-D1 — Roadmap tổng thể (13 phase)
```mermaid
flowchart TB
  P0["Phase 0<br/>Requirement và Skeleton<br/>≈1 tuần"] --> P1["Phase 1<br/>Identity + Gateway<br/>≈2 tuần"]
  P1 --> P2["Phase 2<br/>Catalog + Workflow<br/>≈2–3 tuần"]
  P2 --> P4["Phase 4<br/>Inventory<br/>≈3 tuần"]
  P4 --> P5["Phase 5<br/>Order + Saga sync + gRPC<br/>≈2–3 tuần"]
  P5 --> P6["Phase 6<br/>Kafka + Outbox + Payment + Ticket<br/>≈4 tuần, khó nhất"]
  P6 --> P7["Phase 7<br/>Notification, Audit, SSE<br/>≈2 tuần"]
  P7 --> P8["Phase 8<br/>Waiting Room + Rate Limit<br/>≈3 tuần"]
  P8 --> P9["Phase 9<br/>Observability + Resilience<br/>≈2 tuần"]
  P9 --> P10["Phase 10<br/>Debugging Lab + Perf<br/>≈2–3 tuần"]
  P10 --> P11["Phase 11<br/>Deploy + Public Release<br/>≈2 tuần"]
  P11 --> P12["Phase 12<br/>Advanced Architecture<br/>tùy chọn"]
  P3["Phase 3<br/>Frontend nền<br/>song song, ≈1–2 tuần rải rác"]
  P1 -.-> P3
  P2 -.-> P3
```
Source ước lượng tổng ≈ 20–28 tuần (part-time khoảng 12–15 giờ/tuần).

## C. Detailed diagrams

### S16-D2 — Dependency giữa các phase
> **ĐỀ XUẤT BỔ SUNG:** source chỉ cho thứ tự tuyến tính (và Phase 3 song song). Các cạnh dưới đây suy từ nội dung phase để cho thấy *vì sao* thứ tự đó hợp lý; nét liền = phụ thuộc chính, nét đứt = phụ thuộc phụ.
```mermaid
flowchart TB
  P0["P0 repo, CI, error model, correlation id"] --> P1["P1 auth"]
  P1 --> P2["P2 catalog, ownership"]
  P1 -.-> P3["P3 UI nền"]
  P2 -.-> P3
  P2 --> P4["P4 inventory cần tier và EventPublished"]
  P4 --> P5["P5 order cần Reserve"]
  P5 --> P6["P6 saga async cần order sync + reconciler"]
  P6 --> P7["P7 notification, audit, SSE cần event stream"]
  P7 --> P8["P8 waiting room dùng SSE"]
  P5 -.-> P8
  P1 -.->|"rate limit ở gateway"| P8
  P8 --> P9["P9 observability trên hệ thống đủ thành phần"]
  P6 -.-> P9
  P9 --> P10["P10 debug lab cần trace và metric"]
  P10 --> P11["P11 deploy bản đã tune"]
  P11 --> P12["P12 mở rộng sau v1.0"]
```

### S16-D3 — Cổng chuyển phase (quy tắc khóa phase)
```mermaid
flowchart TB
  S["Hoàn thành code của phase N"] --> Q1{"Đạt toàn bộ DoD của phase N?"}
  Q1 -->|"chưa"| FX["Sửa và bổ sung test hoặc số đo"]
  FX --> Q1
  Q1 -->|"đạt"| Q2{"Qua interview checkpoint của phase N?<br/>trả lời được bằng lý lẽ, không học thuộc"}
  Q2 -->|"chưa"| ST["Học lại phần còn yếu"]
  ST --> Q2
  Q2 -->|"đạt"| NX["Sang phase N+1"]
```

### S16-D4 — Điều kiện cần đạt trước khi sang phase tiếp theo (DoD từng phase)
```mermaid
flowchart TB
  G0["P0: compose up chạy, CI xanh, ADR 1–5, glossary"] --> G1["P1: test bảo mật pass, không secret trong repo, OpenAPI"]
  G1 --> G2["P2: transition sai trả 409 hoặc 422, test ownership, list 1 triệu event nhanh bằng keyset"]
  G2 --> G4["P4: 500 luồng 100 vé đúng 100 chạy 20 lần, expire worker 2 instance không double-release, ADR có số đo"]
  G4 --> G5["P5: không order mồ côi, timeout rõ ràng, REST vs gRPC có số đo"]
  G5 --> G6["P6: chaos script, mọi order về COMPLETED EXPIRED hoặc REFUNDED, không vé mất hoặc trùng, không tiền treo"]
  G6 --> G7["P7: tắt notification vẫn checkout, bật lại gửi bù không trùng"]
  G7 --> G8["P8: 5.000 VU vào phòng chờ ổn định, không bypass token"]
  G8 --> G9["P9: từ 1 correlationId truy ra hành trình, kill 1 service thì alert bắn"]
  G9 --> G10["P10: mỗi bug có postmortem và test chống tái phát"]
  G10 --> G11["P11: tag v1.0.0, deploy 1 lệnh, rollback được, restore backup thử thành công"]
  G3["P3: tự refresh khi hết token, lỗi validation hiện đúng field"] -.-> G2
```

## D. Giải thích sơ đồ
- **D1** là bản đồ thời gian; Phase 3 chạy song song với Phase 1–2 vì UI chỉ cần API đã có.
- **D2** cho thấy tại sao Phase 4 (inventory) đứng trước Phase 6 (Kafka): bạn phải hiểu tranh chấp và dual-write ở dạng đơn giản (sync) trước khi giải bằng event.
- **D3** là quy tắc khóa phase của source: **cả DoD lẫn interview checkpoint** đều phải đạt.
- **D4** gom toàn bộ DoD để bạn có một "bảng cổng" duy nhất.

## E. Những điều phải nhớ
- Phase 6 là phase khó nhất (★★★★★); đừng lướt qua DoD chaos.
- Health + metrics cơ bản bật từ Phase 1; logging JSON + correlation id từ Phase 0 (xem S17).
- **SOURCE CHƯA QUY ĐỊNH:** ánh xạ cạnh dependency (D2 là đề xuất); thời lượng Phase 12.
- **Không khớp nhỏ trong source:** các phase ghi thời lượng cộng lại khoảng 25–28 tuần (bỏ Phase 3 vì song song), trong khi tổng ghi ≈ 20–28 tuần; cận dưới 20 tuần chưa khớp với cách cộng. Nên coi 20–28 chỉ là định hướng.
- Phase 12 trong source nhắc "v1.1 → v4" nhưng bảng mục 23 còn có v5.0.

## F. Liên kết
- Đầu vào: S15. Đầu ra: P00–P12, S17, S18, S19.

## G. Checklist
- [x] 13 phase giữ nguyên thứ tự  - [x] dependency  - [x] gate  - [x] DoD tổng hợp  - [x] thời lượng
