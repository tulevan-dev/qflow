# Glossary QFlow

| Thuật ngữ | Định nghĩa dùng trong QFlow | Chủ sở hữu |
|---|---|---|
| Event | Sự kiện được tổ chức và bán vé; không đồng nghĩa Kafka message | catalog |
| Domain event | Sự kiện nghiệp vụ đã xảy ra, ví dụ `OrderPaid` | service phát sinh |
| Ticket tier | Loại vé của một Event, gồm giá, quota và giới hạn mua | catalog |
| TierStock | Trạng thái tồn kho có tính nhất quán của một tier | inventory |
| Available | `total - reserved - sold`, tính từ nguồn sự thật | inventory |
| Hold / Reservation | Phần vé được giữ tạm cho một order, có TTL | inventory |
| Order | Ý định mua và trạng thái checkout, lưu snapshot giá | order |
| Order item | Dòng vé trong order, gồm tier ID, tên và đơn giá snapshot | order |
| Payment | Giao dịch với provider; không đồng nghĩa order đã hoàn tất | payment |
| Refund | Nghiệp vụ trả lại tiền cho payment | payment |
| Ticket | Quyền tham dự được phát hành sau khi order paid | ticket |
| Check-in | Ghi nhận một ticket đã được sử dụng tại cổng | ticket |
| Waiting room | Hàng đợi điều tiết lượng người được vào checkout | waiting-room |
| Admission token | Bằng chứng được phép vào bước mua, có ký và TTL | waiting-room |
| Organizer profile | Hồ sơ ban tổ chức và trạng thái xác minh | identity |
| Correlation ID | Mã truy vết một request xuyên các thành phần | gateway tạo, mọi service truyền |
| Aggregate | Ranh giới dữ liệu/quy tắc cần nhất quán trong một transaction | từng domain |
| Bounded context | Ranh giới nơi thuật ngữ, quy tắc và ownership có nghĩa thống nhất | kiến trúc |
| Saga | Quy trình nhiều local transaction có điều phối và bù trừ | order |
| Outbox | Bảng lưu event cùng transaction với thay đổi nghiệp vụ | producer service |
| Inbox / processed_events | Dấu vết consumer đã xử lý event để chống lặp | consumer service |
| Idempotency key | Khóa đại diện cho một ý định request, retry không tạo kết quả mới | order/payment |
| Source of truth | Nguồn dữ liệu quyết định cuối cùng | PostgreSQL theo service |

**Phân biệt bắt buộc:** `Event` (sự kiện biểu diễn buổi biểu diễn/workshop) khác `domain event` (thông điệp nghiệp vụ). `Tier` mô tả sản phẩm bán ở catalog; `TierStock` quyết định còn vé ở inventory. `PaymentSucceeded` không có nghĩa ticket đã được phát hành.
