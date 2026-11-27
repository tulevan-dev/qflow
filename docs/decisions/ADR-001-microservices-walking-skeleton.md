# ADR-001 — Microservices từ đầu, nhưng walking skeleton

- **Status:** Accepted for design.
- **Context:** QFlow có workload khác nhau: waiting room throughput cao, inventory consistency cao, payment phụ thuộc bên ngoài, catalog read-heavy. Dự án hướng tới học distributed systems, nhưng khởi tạo toàn bộ service ngay từ đầu sẽ làm tăng chi phí vận hành và trì hoãn giá trị.
- **Decision [SOURCE]:** monorepo microservices từ đầu, **chỉ dựng service theo phase**; Phase 0 Gateway + Hello, Phase 1 Identity; không tạo 10 service runtime ngay.
- **Alternatives:** (1) Modular monolith trước; (2) dựng đầy đủ mọi microservice trước; (3) microservices theo walking skeleton (chọn).
- **Trade-offs:** được ranh giới độc lập và bài học distributed thật, đổi lại nhiều build/deploy config, networking, tracing, contract và eventual consistency hơn monolith.
- **Consequences:** một repo, nhiều module/service; tách DB; API rõ ràng; kiểm thử tích hợp khó hơn. Không dùng Eureka/Config Server/Kubernetes trong giai đoạn này.
