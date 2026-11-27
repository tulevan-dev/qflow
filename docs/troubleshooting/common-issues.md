# Xử lý lỗi thường gặp

Xem trạng thái container và log khi lệnh khởi động báo lỗi.

```bash
docker compose --env-file .env -f infra/compose/compose.yml ps -a
docker compose --env-file .env -f infra/compose/compose.yml logs --tail=100 gateway hello postgres kafka
```

| Triệu chứng | Cách kiểm tra |
| --- | --- |
| Compose báo biến môi trường bắt buộc | Tạo `.env` ở gốc repo và đặt ba mật khẩu theo [README](../../README.md#chạy-local). |
| Port 8080 hoặc 15432 đã được dùng | Kiểm tra tiến trình đang giữ cổng. Nếu chủ động đổi host port trong Compose, dùng cổng mới trong lệnh gọi từ máy host. |
| Hello lỗi xác thực sau khi đổi mật khẩu | PostgreSQL tạo role khi khởi tạo volume lần đầu. Dùng mật khẩu khớp với volume hiện có hoặc cập nhật role từ tài khoản quản trị. |
| Flyway báo checksum mismatch | Kiểm tra lịch sử đã áp dụng theo [Flyway migrations](../database/migrations.md) và khôi phục SQL đã chạy. Đưa thay đổi schema vào migration mới. |
| Gateway chạy nhưng `/hello` lỗi | Kiểm tra trạng thái `hello`, `postgres` và đối chiếu `X-Correlation-Id` giữa log Gateway và Hello. |
| Kafka mất nhiều thời gian để healthy | Xem log Kafka và chờ broker KRaft sẵn sàng. Healthcheck có thời gian khởi động riêng. |

Khi cần tạo lại toàn bộ dữ liệu local, `docker compose --env-file .env -f infra/compose/compose.yml down -v` xóa cả ba volume dữ liệu của stack.
