# QFlow — Mermaid Diagrams

Chuyển `QFLOW_PROJECT_BLUEPRINT` thành hệ thống sơ đồ Mermaid. Giao theo từng phần.

## Trạng thái

| Phần | Phạm vi | Trạng thái |
|---|---|---|
| Part 1 | S00–S05: lý do, product, features, kiến trúc, state machine, database | ✅ Xong (37 sơ đồ) |
| Part 2 | S06–S12: event, security, frontend, observability, failure, testing, performance | ✅ Xong (51 sơ đồ) |
| Part 3 | S13–S26, CL, Phase 0–12, roadmap/dependency/gate | ✅ Xong (55 sơ đồ) |
| Part 4 | Tài liệu triển khai Phase 1 — Identity + Gateway | ✅ Xong (24 sơ đồ, 18 task T00–T17) |

## Cấu trúc

```text
qflow-diagrams/
├── README.md
├── 00-overview/            S00 why, S01 product, S02 features
├── 03-architecture/        S03 backend architecture
├── 04-business-logic/      S04 state machine + checkout + failure paths
├── 05-database/            S05 ERD per service + Redis
├── 06-events/              S06 topic, envelope, outbox, inbox, retry, DLQ, saga
├── 07-security/            S07 token lifecycle, rotation, authz, trust boundary
├── 08-frontend/            S08 sitemap, silent refresh, idempotency, SSE
├── 09-observability/       S09 correlation id, telemetry, dashboards
├── 10-failure-scenarios/   S10 19 tình huống lỗi
├── 11-testing/             S11 test layers, concurrency test
├── 12-performance/         S12 vòng đo, 7 bài toán, benchmark
├── 13-knowledge-map/       S13 bản đồ kiến thức theo nhãn
├── 14-tech-stack/          S14 stack
├── 15-architecture-evolution/  S15 stage A→F
├── 16-roadmap/             S16 tổng quan + P00–P12 (mỗi phase một file)
├── 17-dev-order/           S17 thứ tự build
├── 18-debugging/           S18 12 bug lab
├── 19-interview/           S19 phỏng vấn
├── 20-workflow-docs-adr/   S20 git, docs, ADR
├── 21-deployment/          S21 deploy
├── 22-not-to-build/        S22 overengineering + prioritization
├── 23-expansion/           S23 v1.0 → v5.0
├── 24-skill-progression/   S24 level năng lực
├── 25-public-release/      S25 portfolio
├── 26-mentor/              S26 cách làm việc
├── checklist/              CL Junior skill checklist
├── phase-0-skeleton/           Bộ khởi động Phase 0 (task, ADR gợi mở, DoD)
├── phase-1-identity-gateway/   Tài liệu triển khai Phase 1 (00-README + 01–06)
├── mmd/                    mỗi sơ đồ một file .mmd (sinh bằng tools/validate.mjs)
├── render/                 mỗi sơ đồ một file .svg đã render bằng Chromium (mở bằng trình duyệt)
└── tools/                  validate.mjs (parse + xuất .mmd), render.py (render SVG bằng Playwright)
```

Mỗi file section theo khung A–G: Mục tiêu · Overview · Detailed diagrams · Giải thích · Điều phải nhớ · Liên kết · Checklist.

## Cách xem

- **GitHub / GitLab:** mở file `.md`, Mermaid render tự động.
- **VS Code:** cài extension "Markdown Preview Mermaid Support", mở Preview.
- **Online:** dán nội dung `.mmd` vào https://mermaid.live
- **CLI (tùy chọn):** `npx @mermaid-js/mermaid-cli -i mmd/S04-D5.mmd -o S04-D5.svg`

## Kiểm tra syntax

```bash
npm i mermaid@11 jsdom
node tools/validate.mjs .
```
`validate.mjs` chỉ **parse** cú pháp bằng Mermaid 11. Ngoài ra toàn bộ 172 sơ đồ đã được **render thử trong Chromium** (thư mục `render/`, script `tools/render.py`): không sơ đồ nào lỗi. 16 sơ đồ ban đầu quá rộng (tới ~10.000 px) đã được đổi hướng layout; chỉ một số mẫu được xem bằng mắt, phần còn lại kiểm bằng kích thước. SVG dùng nhãn HTML nên mở bằng trình duyệt.

## Quy ước nhãn

- `ĐỀ XUẤT BỔ SUNG` — nội dung do mình suy ra để lấp khoảng trống, không phải quyết định của source.
- `SOURCE CHƯA QUY ĐỊNH` — source không nêu; không coi là đã chốt.
- Nguồn: `QFLOW_PROJECT_BLUEPRINT.md` (file `.html` nêu trong prompt không được upload).
