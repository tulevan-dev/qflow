---
name: qflow-task-implementation
description: Triển khai task hoặc sửa bug QFlow từ tiêu chí nghiệm thu đến kiểm chứng, có đối chiếu phạm vi của phase và đường chạy hiện tại.
---

# Triển khai task QFlow

1. Xác định hành vi đầu vào và tiêu chí nghiệm thu từ yêu cầu, mã nguồn và tài liệu phase trong [roadmap](../../../docs/diagrams/16-roadmap/S16-roadmap-overview.md). Với Phase 1, tra [danh sách task Identity và Gateway](../../../docs/diagrams/phase-1-identity-gateway/04-implementation-tasks.md).
2. Lần theo entry point hiện tại đến module sở hữu. Khi task đổi API, schema hoặc giao tiếp liên service, mở tài liệu hợp đồng, migration hoặc ADR tương ứng được liên kết trong [AGENTS.md](../../../AGENTS.md). Nếu sửa bug, xác định điều kiện gây lỗi và kiểm tra hồi quy phù hợp.
3. Triển khai theo tiêu chí nghiệm thu. Đồng bộ test, hợp đồng và cấu hình bị ảnh hưởng. Dùng các bước lập kế hoạch, debug hoặc review của Superpowers khi phù hợp với quy mô task.
4. Chạy kiểm tra sát hành vi thay đổi rồi chạy Maven như CI cho thay đổi Java hoặc build. Nếu nghiệm thu đi qua Gateway và nhiều thành phần local, dùng smoke test theo [hướng dẫn Compose](../../../docs/deployment/local-compose.md). Báo thay đổi, kết quả kiểm tra và phần còn cần xác minh.
