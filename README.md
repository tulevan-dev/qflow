# QFlow

QFlow là nền tảng bán vé sự kiện cộng đồng với phòng chờ ảo để điều tiết lượt mua. Phase 0 dựng nền tảng chạy local gồm Gateway, Hello, PostgreSQL, Redis và Kafka.

## Chạy local

Cài Git và Docker có Compose (`docker compose version`). Cài JDK 21 để chạy kiểm thử ngoài container. Repo có sẵn Maven Wrapper.

```bash
git clone --branch develop https://github.com/tulevan-dev/qflow.git
cd qflow
cp .env.example .env
```

Trong PowerShell, dùng `Copy-Item .env.example .env` ở bước cuối. Mở `.env` và đặt mật khẩu local cho `POSTGRES_ADMIN_PASSWORD`, `HELLO_DB_PASSWORD` và `IDENTITY_DB_PASSWORD`.

Từ thư mục gốc, khởi động toàn bộ Phase 0 và chờ các container sẵn sàng bằng một lệnh.

```bash
docker compose --env-file .env -f infra/compose/compose.yml up -d --build --wait
```

```bash
docker compose --env-file .env -f infra/compose/compose.yml ps
curl -i http://127.0.0.1:8080/hello
```

Dùng `curl.exe` trên Windows. Gateway trả HTTP 200 với header `X-Correlation-Id` và JSON có `"service": "hello"`.

## Kiểm thử

```bash
./mvnw -B spotless:check verify
```

Trong PowerShell, chạy `.\mvnw.cmd -B spotless:check verify`.

## Dừng hệ thống

```bash
docker compose --env-file .env -f infra/compose/compose.yml down
```

Thêm `-v` chỉ khi muốn xóa dữ liệu local của cả PostgreSQL, Redis và Kafka.

## Cấu trúc repo

| Thư mục | Nội dung |
| --- | --- |
| `services/gateway`, `services/hello` | Hai module Java đang chạy |
| `infra/compose` | Compose stack, Dockerfile và script khởi tạo PostgreSQL |
| `contracts`, `libs`, `tools`, `web` | Khung cho các phase sau |
| `docs` | Tài liệu dự án |
| `.github/workflows` | CI trên GitHub Actions |

## Tài liệu

- Kiến trúc và thuật ngữ [Tổng quan](docs/architecture/overview.md) · [Glossary](docs/glossary.md)
- HTTP [API Phase 0](docs/api/README.md) · [Chuẩn lỗi](docs/api/error-model.md)
- PostgreSQL [Ownership](docs/database/ownership.md) · [Flyway migrations](docs/database/migrations.md)
- Quyết định kỹ thuật [ADR](docs/decisions/README.md)
- Vận hành local [Docker Compose](docs/deployment/local-compose.md) · [Xử lý lỗi thường gặp](docs/troubleshooting/common-issues.md)
- Số đo CI [Performance journal](docs/learning/perf-journal.md)
