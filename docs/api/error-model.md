# Problem Details và correlation ID

Gateway và Hello định dạng lỗi API bằng `application/problem+json`. Payload thông thường có `type`, `title`, `status`, `detail`, `instance` và `correlationId`. Lỗi validation có thể thêm `errors` với các trường `field`, `code` và `message`.

Client có thể gửi `X-Correlation-Id` ở dạng UUID chuẩn. Gateway giữ ID hợp lệ hoặc tạo UUID mới, chuyển ID đến Hello và trả ID qua response header. Dùng cùng ID để tra log ở cả hai service.

```bash
curl -i -H "X-Correlation-Id: e4eaaaf2-d142-44e1-86b4-dc0f58afc8db" http://127.0.0.1:8080/does-not-exist
```

Request trên trả HTTP 404. Trường `correlationId` trong body khớp với `X-Correlation-Id` trong header.
