# PHASE 1 — Identity + Gateway: Tài liệu triển khai

Nguồn: Phase 1 (S16), S07, S05 §5.1, S01. Thời lượng ≈ 2 tuần, độ khó ★★★☆☆.

## Bản đồ 20 mục
| # | Mục yêu cầu | Nơi trả lời |
|---|---|---|
| 1 | Mục tiêu, phạm vi, tiêu chí hoàn thành | `01-scope-architecture.md` |
| 2 | Kiến trúc gateway và identity-service | `01-scope-architecture.md` |
| 3 | Register/login flow | `02-flows.md` mục 3 |
| 4 | JWT RS256 và JWKS verification | `02-flows.md` mục 4 |
| 5 | Vòng đời access token và refresh token | `02-flows.md` mục 5 |
| 6 | Refresh rotation và reuse detection | `02-flows.md` mục 6 |
| 7 | Logout và revocation | `02-flows.md` mục 7 |
| 8 | RBAC và method-level authorization | `02-flows.md` mục 8 |
| 9 | Organizer profile | `02-flows.md` mục 9 |
| 10 | Login rate limiting | `02-flows.md` mục 10 |
| 11 | Data model và token-family design | `03-data-api.md` mục 11 |
| 12 | API contract và error cases | `03-data-api.md` mục 12 |
| 13 | Trình tự triển khai theo task | `04-implementation-tasks.md` (T00–T17) |
| 14 | Unit, integration, security test | `05-testing.md` mục 14 |
| 15 | Negative và abuse cases | `05-testing.md` mục 15 |
| 16 | Definition of Done kiểm chứng được | `05-testing.md` mục 16 |
| 17 | Lỗi triển khai thường gặp | `06-checklists-pitfalls-selfcheck.md` mục 17 |
| 18 | Checklist local, test, review | `06-checklists-pitfalls-selfcheck.md` mục 18 |
| 19 | Chưa thuộc Phase 1 | `06-checklists-pitfalls-selfcheck.md` mục 19 |
| 20 | Câu hỏi tự kiểm tra | `06-checklists-pitfalls-selfcheck.md` mục 20 |

## Cách dùng
1. Đọc 01 → 03 (thiết kế, luồng, dữ liệu, API).
2. Làm T01 trước: chốt 11 quyết định thành ADR.
3. Làm lần lượt T00 → T17; mỗi task phải có test xanh trước khi sang task sau.
4. Nghiệm thu bằng 05 mục 16; tự trả lời 20 câu ở 06.

## Quy ước nhãn
- **ĐỀ XUẤT BỔ SUNG:** mình bổ sung để lấp khoảng trống triển khai, không phải quyết định của source.
- **SOURCE CHƯA QUY ĐỊNH:** source không nêu; bạn tự chốt và ghi ADR.
- Toàn bộ endpoint, tên bảng/cột phụ, tên test và đường dẫn file ở đây là đề xuất; DoD và tính năng lấy từ source.

## Sơ đồ
24 sơ đồ Mermaid (PH1-D1 … PH1-D24), nằm trong các file 01, 02, 03, 04, 05. Đã được parser Mermaid 11 kiểm cú pháp; chưa render ra hình.
