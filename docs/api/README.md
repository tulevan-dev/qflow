# API Phase 0

Với Docker Compose, gọi Gateway qua `http://127.0.0.1:8080`.

Mở [Swagger UI](http://127.0.0.1:8080/swagger-ui.html) sau khi stack Compose sẵn sàng. Chọn `GET /hello`, bấm **Try it out** rồi **Execute** để gọi Gateway. Swagger UI đọc hợp đồng được đóng gói tại `/openapi-phase0.yaml`.

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

## Kiểm tra OpenAPI

Kiểm tra hợp đồng với ruleset `spec` của Redocly CLI. Trên Windows, dùng `npx.cmd`.

```bash
npx --yes @redocly/cli@2.12.0 lint --extends spec docs/api/openapi-phase0.yaml
```

- [Chuẩn lỗi và correlation ID](error-model.md)
- [OpenAPI Phase 0](openapi-phase0.yaml)
- [Thiết kế API contract cho P0-T11](../diagrams/phase-0-skeleton/QFLOW-PHASE-0-COMPLETE.md#p0-t11--api-contract-đầu-tiên)
