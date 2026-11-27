---
name: qflow-flyway
description: Thêm hoặc rà soát migration SQL Flyway của QFlow, kiểm tra version, checksum hoặc lỗi nâng cấp schema PostgreSQL.
---

# Flyway QFlow

1. Đọc [quy ước migration](../../../docs/database/migrations.md) và [database ownership](../../../docs/database/ownership.md). Kiểm tra POM, cấu hình Flyway và thư mục `src/main/resources/db/migration/` của service sở hữu.
2. Chọn version số nguyên tiếp theo chưa dùng trong service theo mẫu `V<n>__<description>.sql`. Kiểm tra trùng version với nhánh đích nếu thông tin đã có. Hello giữ nguyên `V1__baseline.sql` đã áp dụng.
3. Giữ nguyên nội dung, tên và version các script đã áp dụng. Với checksum mismatch, so sánh file với lịch sử Git và `flyway_schema_history` trên đúng DB. Khôi phục script gốc khi file bị sửa ngoài ý muốn và đưa thay đổi schema vào migration mới. Chỉ cân nhắc `repair` sau khi xác minh schema thực tế và có phê duyệt cho database đó.
4. Kiểm chứng trên PostgreSQL của service ở môi trường thử nghiệm. Với thay đổi ảnh hưởng dữ liệu cũ, chạy cả đường nâng cấp từ schema trước với dữ liệu đại diện, ngoài đường khởi tạo DB mới. Xác nhận Flyway ghi đúng version thành công và ứng dụng đọc ghi theo schema mới.
5. File trong `infra/compose/postgres/init/` chạy khi khởi tạo volume. Dùng Flyway cho thay đổi schema cần áp dụng trên database đã tồn tại.
