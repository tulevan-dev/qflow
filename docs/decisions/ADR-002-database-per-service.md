# ADR-002 — Database-per-service, cấm cross-DB access

- **Status:** Accepted for design.
- **Context:** Shared DB khiến service phụ thuộc schema và quyền ghi của nhau; deploy không độc lập.
- **Decision [SOURCE]:** PostgreSQL 16 một instance dev, **nhiều database logic** với **user riêng**, chỉ owner service được đọc/ghi DB của mình. Không cross-service FK, join hoặc dùng một DB user cho mọi service.
- **Alternatives:** (1) Một shared DB/schema; (2) một PostgreSQL instance cho mỗi service ngay từ đầu; (3) một instance, DB/user riêng (chọn cho dev).
- **Trade-offs:** cần API/read model và chấp nhận eventual consistency; một PostgreSQL instance vẫn là single point of failure ở dev, chưa phải isolation hạ tầng hoàn toàn.
- **Consequences:** Phase 0 tạo `hello_db` và `identity_db`, với `hello_app` và `identity_app`; các DB khác tạo khi service xuất hiện. `hello_app` không thể kết nối `identity_db` (cần kiểm chứng thực tế). Không dùng superuser cho application.
