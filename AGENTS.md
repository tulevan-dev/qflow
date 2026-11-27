# QFlow

## Phạm vi và kiến trúc

- Kiểm tra module trong `pom.xml` và tài liệu của phase trước khi sửa. Gateway và Hello là hai module Phase 0. Đọc [tổng quan kiến trúc](docs/architecture/overview.md) và [ADR](docs/decisions/README.md) khi thay đổi ranh giới service.
- Gateway dùng Spring Cloud Gateway WebFlux, phụ trách routing và xử lý ở biên. Hello dùng Spring MVC và Spring Data JDBC. Giữ logic nghiệp vụ và dữ liệu trong service sở hữu. Các service giao tiếp qua API hoặc event, mỗi service quản lý database, role và lịch sử Flyway riêng. Xem [database ownership](docs/database/ownership.md).
- Khi thay đổi HTTP API, đồng bộ hợp đồng OpenAPI của phase, implementation và kiểm thử. Giữ định dạng Problem Details và quy tắc truyền `X-Correlation-Id` theo [chuẩn lỗi](docs/api/error-model.md).
- Giữ nguyên migration đã áp dụng. Đưa thay đổi schema vào migration versioned mới trong module sở hữu. Xem [quy ước Flyway](docs/database/migrations.md).

## Build và kiểm chứng

- Dùng Java 21 và Maven Wrapper ở root. Đọc parent POM, POM module và test lân cận trước khi dùng dependency, import hoặc annotation từ ví dụ cộng đồng. Repo hiện dùng Spring Boot 4.1.2. Nếu cần thay bean trong test Spring, kiểm tra `@MockitoBean` của Spring Test thay cho `@MockBean` trong ví dụ cũ.
- Với thay đổi Java hoặc build, chạy lệnh tương ứng CI từ root. PowerShell dùng `.\mvnw.cmd -B spotless:check verify`, Bash dùng `./mvnw -B spotless:check verify`. Khi chỉ sửa tài liệu hoặc cấu hình agent, kiểm tra định dạng, liên kết và cấu hình liên quan. Báo rõ các kiểm tra thực sự đã chạy.
- Dùng [.env.example](.env.example) và [hướng dẫn Compose](docs/deployment/local-compose.md) để cấu hình local. Giữ thông tin xác thực ngoài Git và log.

## Cách dùng skill

- Dùng tài liệu và mã nguồn QFlow làm nguồn chuẩn. Superpowers hỗ trợ quy trình khi phù hợp với yêu cầu. ECC hỗ trợ tra cứu Java và Spring Boot, với ví dụ được đối chiếu theo module và phiên bản trong POM.
- Các skill QFlow trong `.agents/skills/` hướng dẫn thay đổi kiến trúc, migration, review, testing, Git và triển khai task. Áp dụng hướng dẫn theo phần việc thực tế.

- Superpowers và ba skill ECC được lưu trong `.agents/skills/` của repo để dùng chung khi clone. Có thể gọi trực tiếp theo tên skill, ví dụ `systematic-debugging` hoặc `springboot-tdd`. Với Superpowers, các tham chiếu dạng `superpowers:<tên>` trỏ đến skill cùng tên trong `.agents/skills/superpowers/`. Khi cần dùng quy trình Superpowers, đọc `using-superpowers` trong bộ skill này.
- Nguồn vendored gồm [Superpowers v7.0.1](https://github.com/obra/superpowers/tree/2ce8aa15d05bd57213e6f746f953aafb7ded7a8b/skills) và [ECC](https://github.com/affaan-m/ECC/tree/4adeee6732a12d1540f49a4d50a38ce8c97f9a53/skills). Giấy phép nằm cùng từng bộ.
