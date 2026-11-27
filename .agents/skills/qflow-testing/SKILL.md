---
name: qflow-testing
description: Viết hoặc chạy unit test, integration test và kiểm thử hồi quy cho module Java, routing HTTP hoặc PostgreSQL của QFlow.
---

# Kiểm thử QFlow

- Bắt đầu từ hành vi cần xác nhận và test gần nhất. Dùng JUnit cho logic thuần, rồi chọn mức tích hợp khi cần xác minh hành vi do Spring, HTTP hoặc database cung cấp.
- Hello dùng MVC và có ví dụ `RestTestClient` trong `HelloProblemDetailsTests`. Gateway dùng WebFlux và có ví dụ `WebTestClient` với upstream local trong `GatewayProblemDetailsTests`. Đối chiếu starter và package theo POM module trước khi áp dụng ví dụ ECC.
- Với test SQL, migration, constraint hoặc transaction phụ thuộc PostgreSQL, dùng PostgreSQL thử nghiệm. Xem [hướng dẫn Compose](../../../docs/deployment/local-compose.md) và [hướng dẫn migration](../../../docs/database/migrations.md). Kiểm tra cùng điều kiện dữ liệu như hành vi cần bảo vệ.
- Chạy từ root. Ví dụ nhắm vào Gateway bằng `.\mvnw.cmd -B -pl services/gateway -am test`, sau đó chạy `.\mvnw.cmd -B spotless:check verify` cho thay đổi Java hoặc build. Trên Bash dùng `./mvnw`. Báo rõ phần nào phụ thuộc Docker và đã được kiểm chứng. Maven là gate của [CI hiện hành](../../../.github/workflows/ci.yml).
