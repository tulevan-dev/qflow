# API Phase 0

Với Docker Compose, gọi Gateway qua `http://127.0.0.1:8080`.

| Method và path | Kết quả |
| --- | --- |
| `GET /hello` | Gateway chuyển tiếp tới Hello. HTTP 200 trả JSON với `service` và `message`. |
| `GET /actuator/health` | Health của Gateway. Hello có health riêng bên trong mạng Compose. |

```json
{
  "service": "hello",
  "message": "QFlow skeleton is running"
}
```

- [Chuẩn lỗi và correlation ID](error-model.md)
- [Thiết kế API contract cho P0-T11](../diagrams/phase-0-skeleton/QFLOW-PHASE-0-COMPLETE.md#p0-t11--api-contract-đầu-tiên)
