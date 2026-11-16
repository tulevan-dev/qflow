# S13 — Knowledge Map

> Nguồn: mục "13. KNOWLEDGE MAP (map kiến thức → feature)". Nhãn: `CORE` · `USEFUL` · `ADVANCED` · `OPTIONAL` · `OVERENGINEERING`.

## A. Mục tiêu
- Biết kiến thức nào là bắt buộc, đáng học, nâng cao, tùy chọn hay là over-engineering, và học nó ở đâu trong project.
- Liên hệ: S16 (phase), S22 (MUST/SHOULD/NICE/DO NOT BUILD), checklist cuối tài liệu.

## B. Overview diagram

### S13-D0 — Bảy nhóm kiến thức
```mermaid
flowchart LR
  K["Knowledge Map"] --> J["Java"]
  K --> SP["Spring Boot"]
  K --> A["API"]
  K --> D["Database"]
  K --> DS["Distributed Systems"]
  K --> AR["Architecture"]
  K --> IS["Infrastructure và Security"]
```

## C. Detailed diagrams

### S13-D1 — Java
```mermaid
mindmap
  root((Java))
    CORE
      OOP Generics Exception design
      Collections Stream Optional
      Concurrency ExecutorService CompletableFuture
      synchronized Lock atomic so với DB lock
    USEFUL
      JVM heap GC thread dump heap dump
    ADVANCED
      Virtual threads
```
| Kiến thức | Nhãn | Học ở đâu |
|---|---|---|
| OOP, Generics, Exception design | CORE | domain model, error hierarchy |
| Collections, Stream, Optional | CORE | mapping, aggregation báo cáo |
| Concurrency, ExecutorService, CompletableFuture | CORE | concurrency test, outbox relay, parallel call có timeout |
| `synchronized`/`Lock`/atomic vs DB lock | CORE | hiểu vì sao lock JVM vô dụng khi có nhiều instance |
| JVM: heap/GC, thread dump, heap dump | USEFUL | debug memory leak (bug #4) |
| Virtual threads | ADVANCED | waiting-room SSE |

### S13-D2 — Spring Boot
```mermaid
mindmap
  root((Spring Boot))
    CORE
      DI bean lifecycle Configuration profiles
      Validation và RestControllerAdvice
      Spring Security resource server method security
      Transactional propagation rollback self-invocation
      JPA Hibernate lazy N+1 Version
      Scheduled và ShedLock
      Actuator và Micrometer
      Spring Kafka error handler retry topic DLT
    USEFUL
      Spring Cloud Gateway
      Resilience4j
    OVERENGINEERING
      Spring Cloud Config và Eureka
```
| Kiến thức | Nhãn | Ở đâu |
|---|---|---|
| DI, bean lifecycle, `@Configuration`, profiles | CORE | mọi service |
| Validation, `@RestControllerAdvice` | CORE | error model chuẩn |
| Spring Security (resource server, method security) | CORE | identity + mọi service |
| `@Transactional` (propagation, rollback rule, self-invocation) | CORE | order/inventory |
| JPA/Hibernate (lazy, N+1, `@Version`) | CORE | catalog/order |
| `@Scheduled` + ShedLock | CORE | expire hold, reconciler, outbox |
| Actuator, Micrometer | CORE | observability |
| Spring Cloud Gateway | USEFUL | gateway |
| Spring Kafka (error handler, retry topic, DLT) | CORE | consumers |
| Resilience4j (timeout, retry, CB, bulkhead) | USEFUL | order→catalog/inventory |
| Spring Cloud Config/Eureka | OVERENGINEERING | — |

### S13-D3 — API
```mermaid
mindmap
  root((API))
    CORE
      REST HTTP status code
      Cursor pagination filter sort
      Idempotency
      Error response problem+json RFC 7807
      Versioning api v1
      OpenAPI
    OPTIONAL
      GraphQL không cần
    OVERENGINEERING
      HATEOAS
```

### S13-D4 — Database
```mermaid
mindmap
  root((Database))
    CORE
      SQL nâng cao
      Index composite partial GIN
      Transaction và isolation level
      Optimistic và pessimistic lock
      SKIP LOCKED
      EXPLAIN ANALYZE
      N+1
      Connection pool
      Flyway
    ADVANCED
      Partitioning
      Advisory lock
    OVERENGINEERING
      Sharding
```

### S13-D5 — Distributed Systems
```mermaid
mindmap
  root((Distributed Systems))
    CORE
      CAP và consistency model
      Eventual consistency
      Retry timeout backoff jitter
      Idempotency
      Duplicate message
      Ordering theo partition key
      DLQ
      Saga
      Outbox và Inbox
    USEFUL
      Circuit breaker bulkhead
      CQRS-lite read model
    OPTIONAL
      Consensus Raft đọc để hiểu
    OVERENGINEERING
      Event sourcing ở giai đoạn này
```

### S13-D6 — Architecture
```mermaid
mindmap
  root((Architecture))
    CORE
      Layered và Hexagonal nhẹ
      DDD cơ bản aggregate bounded context ubiquitous language
      Microservices boundary
      Event-driven
    USEFUL
      API Gateway hoặc BFF
    OVERENGINEERING
      Clean Architecture đủ 4 vòng cho mọi service
```

### S13-D7 — Infrastructure và Security
```mermaid
mindmap
  root((Infra và Security))
    CORE hoặc USEFUL
      Docker và Compose healthcheck
      Kafka basics
      Redis data structures
      Reverse proxy Caddy
      GitHub Actions
    CORE bảo mật
      JWT refresh rotation
      RBAC và ownership check
      Password hashing
      CORS CSRF
      Rate limit
      Secret management
    OPTIONAL
      OAuth2 OIDC social login
    OVERENGINEERING hiện tại
      Terraform K8s Helm Istio
```

## D. Giải thích sơ đồ
- Mỗi nhánh gom kiến thức theo **nhãn** để bạn thấy ngay đâu là bắt buộc. Cột "học ở đâu" (bảng Java, Spring) trỏ kiến thức tới feature cụ thể của project.
- `CORE` nghĩa là thiếu thì chưa đạt Junior; `OVERENGINEERING` nghĩa là đừng đưa vào project ở giai đoạn này (xem S22).

## E. Những điều phải nhớ
- Không ép mọi thứ vào project; học theo nhãn và theo phase.
- Lock JVM (`synchronized`) vô dụng khi nhiều instance; phải dùng ràng buộc/lock ở DB.
- **SOURCE CHƯA QUY ĐỊNH:** thứ tự học từng kiến thức trong từng phase (chỉ có "học ở đâu" cho Java và Spring); mục "Infrastructure & Security" gom "CORE/USEFUL" mà không nêu từng mục thuộc nhãn nào.

## F. Liên kết
- Đầu vào: S03, S04–S12. Đầu ra: S16 (phase), S22, Skill Checklist.

## G. Checklist
- [x] Java  - [x] Spring Boot  - [x] API  - [x] Database  - [x] Distributed  - [x] Architecture  - [x] Infra & Security  - [x] đủ 5 nhãn
