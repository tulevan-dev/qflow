# S21 — Deployment Strategy

> Nguồn: mục "21. DEPLOYMENT STRATEGY".

## A. Mục tiêu
- Lộ trình triển khai chi phí thấp: Local → CI → Staging → Production trên một VPS, không cần Kubernetes.
- Liên hệ: S14 (stack), S20 (CI), S05 (migration), Phase 11.

## B. Overview diagram

### S21-D1 — Lộ trình môi trường
```mermaid
flowchart LR
  L["Local<br/>Docker Compose"] --> CI["CI<br/>test + build image"] --> ST["Staging<br/>VPS nhỏ hoặc cùng VPS khác port"] --> PR["Production<br/>VPS + Caddy + Cloudflare"]
```

## C. Detailed diagrams

### S21-D2 — Pipeline CI/CD
```mermaid
sequenceDiagram
  autonumber
  participant Dev as Developer
  participant GH as GitHub Actions
  participant REG as GHCR
  participant VPS as VPS
  Dev->>GH: push hoặc merge vào main
  GH->>GH: lint
  GH->>GH: unit test
  GH->>GH: integration test với Testcontainers
  GH->>GH: build image
  GH->>REG: push image
  GH->>VPS: SSH deploy docker compose pull và up -d
  GH->>VPS: smoke test
  alt smoke test fail
    GH->>VPS: rollback về tag trước
  else pass
    VPS-->>Dev: deploy thành công
  end
```

### S21-D3 — Migration an toàn
```mermaid
flowchart LR
  E["Expand<br/>thêm cột hoặc bảng mới, tương thích ngược"] --> M["Migrate<br/>chuyển dữ liệu và code sang dùng cái mới"] --> C["Contract<br/>gỡ cái cũ khi không còn dùng"]
  X["Đổi tên cột kiểu breaking"] -.->|"KHÔNG"| E
```

### S21-D4 — Khi nào mới cần Kubernetes
```mermaid
flowchart TB
  Q{"Cần autoscale thật theo tải<br/>hoặc nhiều node<br/>hoặc rolling deploy phức tạp?"} -->|"không"| C["Giữ Docker Compose trên VPS"]
  Q -->|"có"| K["Cân nhắc Kubernetes<br/>đã có Docker image sẵn nên chuyển được"]
```

## D. Giải thích sơ đồ
- **D1–D2:** mọi thay đổi đi qua test trước khi tạo image; deploy bằng SSH + Compose và có smoke test; fail thì rollback tag trước.
- **D3:** expand → migrate → contract để migration không phá bản đang chạy (tránh downtime).
- **D4:** chỉ xét K8s khi có nhu cầu thật; hiện tại là overengineering.

## E. Những điều phải nhớ
- Cấu hình gợi ý: 1 VPS 4 vCPU / 8GB (Hetzner, Contabo, DigitalOcean…) chạy toàn bộ stack bằng Compose.
- Nếu thiếu RAM: gộp Postgres thành 1 instance nhiều DB, dùng Redpanda thay Kafka, tắt Loki.
- Next.js: Vercel/Cloudflare hoặc container cùng VPS; HTTPS/WAF/DNS: Caddy + Cloudflare.
- Backup: `pg_dump` định kỳ ra object storage và **thử restore**.
- Không mở port Postgres/Kafka ra Internet; không để secret trong image (Phase 11).
- **SOURCE CHƯA QUY ĐỊNH:** tần suất backup, chiến lược staging cụ thể (cùng VPS khác port là gợi ý), cách rollback dữ liệu khi migration đã chạy, nhà cung cấp object storage.

## F. Liên kết
- Đầu vào: S14, S20. Đầu ra: Phase 11, S25.

## G. Checklist
- [x] 4 môi trường  - [x] pipeline  - [x] migration  - [x] backup  - [x] lý do không K8s
