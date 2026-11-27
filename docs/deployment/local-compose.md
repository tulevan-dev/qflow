# Docker Compose local

Thực hiện từ thư mục gốc repo. Tạo `.env` theo [README](../../README.md#chạy-local), rồi chạy lệnh dưới đây.

```bash
docker compose --env-file .env -f infra/compose/compose.yml up -d --build --wait
docker compose --env-file .env -f infra/compose/compose.yml ps
```

| Service | Địa chỉ |
| --- | --- |
| Gateway | `127.0.0.1:8080` trên máy host |
| Hello | `hello:8081` trong mạng Compose |
| PostgreSQL | `127.0.0.1:15432` trên host và `postgres:5432` trong mạng Compose |
| Redis | `redis:6379` trong mạng Compose |
| Kafka KRaft | `kafka:19092` trong mạng Compose |

Kiểm tra Gateway và đường gọi tới Hello. Trên Windows, dùng `curl.exe`.

```bash
curl -fsS http://127.0.0.1:8080/actuator/health
curl -i http://127.0.0.1:8080/hello
```

`down` dừng stack và giữ dữ liệu. Xem [xử lý lỗi thường gặp](../troubleshooting/common-issues.md) nếu container chưa healthy.
