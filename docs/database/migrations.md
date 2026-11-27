# Flyway migrations

Hello chạy Flyway lúc khởi động và lưu lịch sử trong `public.flyway_schema_history` của `hello_db`. [V1 baseline](../../services/hello/src/main/resources/db/migration/V1__baseline.sql) tạo bảng `hello_metadata` và ghi dòng `service_name` với giá trị `hello-service`.

Thêm thay đổi dưới dạng file SQL mới trong `services/hello/src/main/resources/db/migration/`, ví dụ `V2__add_example.sql`. Giữ nguyên các file đã được áp dụng để Flyway xác thực checksum trên các môi trường cũ. Identity sẽ có lịch sử migration riêng khi module được triển khai.

Sau khi khởi động stack theo [hướng dẫn local](../deployment/local-compose.md), xem lịch sử bằng quyền DB owner. PostgreSQL sẽ hỏi mật khẩu `HELLO_DB_PASSWORD` trong `.env`.

```bash
docker compose --env-file .env -f infra/compose/compose.yml exec postgres psql -h 127.0.0.1 -U hello_app -d hello_db -c "select version, description, success from public.flyway_schema_history order by installed_rank;"
```
