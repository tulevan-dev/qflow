# Database ownership

Phase 0 dùng một instance PostgreSQL local với DB và role riêng cho mỗi service.

| Database | Owner | Sử dụng |
| --- | --- | --- |
| `hello_db` | `hello_app` | Hello chạy Flyway và sở hữu schema của mình. |
| `identity_db` | `identity_app` | Chuẩn bị cho Identity ở Phase 1. |

Gateway chỉ route HTTP. Service truy cập dữ liệu của context khác qua API hoặc event theo [ADR-002](../decisions/ADR-002-database-per-service.md).

[Script khởi tạo](../../infra/compose/postgres/init/01-create-databases.sh) tạo role, database và thu hồi quyền `CONNECT` của `PUBLIC` trên hai database ứng dụng. PostgreSQL chạy script này khi volume dữ liệu còn trống. Tài khoản `postgres` dùng cho tác vụ quản trị.
