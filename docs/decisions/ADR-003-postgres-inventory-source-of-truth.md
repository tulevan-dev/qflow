# ADR-003 — PostgreSQL là source of truth của inventory

- **Status:** Accepted for design.
- **Context:** Redis có thể mất cache/TTL; bán vượt quota là lỗi nghiêm trọng.
- **Decision [SOURCE]:** PostgreSQL lưu `total`, `reserved`, `sold`; reserve bằng atomic conditional update và CHECK constraint. Redis chỉ dùng cache hiển thị availability, không quyết định còn vé.
- **Alternatives:** (1) Redis counter làm nguồn sự thật; (2) PostgreSQL atomic update (chọn trước); (3) PostgreSQL optimistic locking / `SELECT FOR UPDATE`.
- **Trade-offs:** contention hot row ở Postgres khi flash sale; ưu tiên correctness hơn throughput, chỉ tối ưu sau khi đo.
- **Consequences:** cache mất thì rebuild; inventory DB down thì fail fast; Phase 4 cần concurrency test và so sánh chiến lược bằng số đo thật. **Không code inventory ở Phase 0.**
