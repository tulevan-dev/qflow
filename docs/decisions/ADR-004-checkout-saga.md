# ADR-004 — Orchestrated saga cho checkout, choreography cho side effects

- **Status:** Accepted for design.
- **Context:** Checkout qua inventory/payment/ticket có nhiều điểm lỗi; email/audit không được chặn checkout.
- **Decision [SOURCE]:** `order-service` điều phối saga và state machine; notification/audit/analytics chỉ consume event, không quyết định state checkout.
- **Alternatives:** (1) Distributed 2PC/XA; (2) choreography cho mọi bước; (3) orchestration cho critical path + choreography side effects (chọn).
- **Trade-offs:** order-service phức tạp hơn, là điểm điều phối logic; đổi lại state, retry, compensation và debug dễ truy vết hơn choreography thuần.
- **Consequences:** order có state `PENDING`, `AWAITING_PAYMENT`, `PAID`, `COMPLETED`, `EXPIRED`, `REFUND_PENDING`, `REFUNDED`…; late payment sau expiry chuyển sang refund **[CHỐT THIẾT KẾ]**; reconciliation là bắt buộc ở phase checkout. **Không code saga ở Phase 0.**
