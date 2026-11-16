# S24 — Skill Progression: Fresher → Junior

> Nguồn: mục "24. SKILL PROGRESSION".

## A. Mục tiêu
- Map project với 4 cấp năng lực và nói rõ những gì project **không** thay thế được.
- Liên hệ: S16 (phase), S19 (interview), checklist.

## B. Overview diagram

### S24-D1 — Bốn level và phase luyện
```mermaid
flowchart LR
  F["Fresher<br/>CRUD, JPA cơ bản, REST, SQL cơ bản<br/>Phase 0 đến 2"] --> SF["Strong Fresher<br/>Auth đúng, transaction, index, test có DB thật, Docker<br/>Phase 1–2 và 4"]
  SF --> J["Junior<br/>Concurrency, idempotency, outbox, retry, event duplicate<br/>đọc log và trace để debug, viết test đáng tin, review code của mình<br/>Phase 4–7 và 9–10"]
  J --> SJ["Strong Junior<br/>Quyết định kiến trúc có trade-off, đo trước khi tối ưu<br/>vận hành được, chống overengineering<br/>Phase 8–11 + ADR + perf journal"]
```

## C. Detailed diagrams

### S24-D2 — Những gì project không thay thế được và cách bù
```mermaid
flowchart LR
  subgraph GAP["Không thay thế được kinh nghiệm thật"]
    G1["Codebase lớn hoặc legacy do người khác viết"]
    G2["Incident thật lúc 2h sáng với dữ liệu thật và áp lực"]
    G3["Review từ đồng nghiệp có kinh nghiệm hơn"]
    G4["Tải và dữ liệu thật ở quy mô công ty, chi phí và vận hành lâu dài, làm việc theo team và deadline"]
  end
  subgraph FILL["Cách bù"]
    B1["Public repo"]
    B2["Mời người ngoài review"]
    B3["Chạy sự kiện thật với 1 CLB hoặc nhóm bạn"]
    B4["Viết postmortem"]
  end
  GAP --> FILL
```

## D. Giải thích sơ đồ
- D1: cấp càng cao càng dịch từ "làm được" sang "giải thích, đo, quyết định và vận hành".
- D2: nhận thức giới hạn của project cá nhân và có biện pháp bù cụ thể.

## E. Những điều phải nhớ
- Các phase ở D1 chồng lấn (ví dụ Phase 4 xuất hiện ở hai level) vì cùng một phase luyện nhiều năng lực.
- **SOURCE CHƯA QUY ĐỊNH:** tiêu chí đo để xác nhận đã lên level (source dùng checklist ở cuối làm công cụ tự đánh giá).

## F. Liên kết
- Đầu vào: S16. Đầu ra: Skill Checklist.

## G. Checklist
- [x] 4 level  - [x] project giúp luyện gì  - [x] giới hạn và cách bù
