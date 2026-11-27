# ADR-005 — Transactional Outbox + idempotent Inbox, không dual-write

- **Status:** Accepted for design.
- **Context:** Commit DB rồi publish Kafka có cửa sổ crash làm mất event; publish trước commit có thể phát sự kiện cho thay đổi DB chưa thành công.
- **Decision [SOURCE]:** producer ghi business data + outbox trong cùng DB transaction; relay publish at-least-once; consumer ghi `processed_events` và thay đổi nghiệp vụ trong cùng transaction.
- **Alternatives:** (1) Ghi DB rồi publish trực tiếp; (2) XA/2PC; (3) Outbox poller + Inbox (chọn); Debezium CDC để sau.
- **Trade-offs:** cần outbox table, relay, retry, monitoring và cleanup; event có thể đến trễ hoặc trùng, không đảm bảo exactly-once end-to-end.
- **Consequences:** envelope có `eventId`, `type`, `version`, `occurredAt`, `aggregateId`, `correlationId`, `causationId`, `payload`; partition key theo aggregate; consumer idempotent; DLQ/replay ở phase async. **Kafka chạy healthy ở Phase 0 nhưng chưa có producer/consumer nghiệp vụ.**
