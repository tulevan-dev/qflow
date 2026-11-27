---
name: qflow-architecture
description: Thiết kế hoặc thay đổi ranh giới microservice, quyền sở hữu dữ liệu, giao tiếp liên service hoặc routing trong QFlow.
---

# Kiến trúc QFlow

1. Đối chiếu [kiến trúc đang chạy](../../../docs/architecture/overview.md), [context map](../../../docs/architecture/context-map.md), [database ownership](../../../docs/database/ownership.md) và tài liệu phase liên quan. Dùng POM và cấu hình runtime để xác nhận các module đã triển khai.
2. Xác định service sở hữu hành vi, dữ liệu và transaction. Giữ Gateway WebFlux ở biên. Mỗi service quản lý dữ liệu của mình và truy cập context khác qua API hoặc event theo [ADR-002](../../../docs/decisions/ADR-002-database-per-service.md).
3. Với thay đổi liên service, xác định hợp đồng và hành vi khi upstream lỗi. Khi có xử lý lặp hoặc bất đồng bộ, rà idempotency và cách khôi phục phù hợp. Đọc [ADR](../../../docs/decisions/README.md) liên quan trước khi thay đổi checkout, inventory hoặc outbox.
4. Đồng bộ hợp đồng, cấu hình và tài liệu bị ảnh hưởng. Ghi ADR theo cấu trúc hiện có nếu task đưa ra quyết định kiến trúc mới có hệ quả dài hạn.
