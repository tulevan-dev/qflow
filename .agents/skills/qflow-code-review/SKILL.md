---
name: qflow-code-review
description: Review diff, branch hoặc pull request QFlow để tìm lỗi hành vi, sai ranh giới service, thay đổi contract, migration và thiếu kiểm thử hồi quy.
---

# Review QFlow

1. Xác định diff và nhánh đích từ yêu cầu hoặc PR. Đọc code gọi đến phần sửa, hợp đồng API và test ở đường chạy bình thường.
2. Ưu tiên lỗi có tác động quan sát được. Rà thay đổi tương thích HTTP, Problem Details và correlation ID, ownership database, checksum migration và API Spring Boot 4 thực tế. Với thay đổi ở Gateway, rà các thao tác blocking trong đường xử lý WebFlux. Với luồng dữ liệu hoặc event, kiểm tra transaction, retry và trạng thái khi xử lý dở nếu diff tác động đến chúng.
3. Đối chiếu test với hành vi thay đổi và dữ liệu đã tồn tại. Chỉ ra kiểm tra còn thiếu khi nó giúp bắt một lỗi cụ thể.
4. Trình bày phát hiện theo mức độ, kèm file và dòng, tình huống xảy ra, tác động và cách sửa. Báo phạm vi diff và các kiểm tra đã chạy. Với yêu cầu chỉ review, giữ kết quả dưới dạng nhận xét.
