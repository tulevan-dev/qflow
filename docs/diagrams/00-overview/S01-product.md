# S01 — Product (Problem, Solution, Target users, User roles)

> Nguồn: mục "1. PRODUCT" (1.1–1.4).

## A. Mục tiêu
- Chốt vấn đề, giải pháp, nhóm người dùng và **7 role** với trách nhiệm nghiệp vụ thật.
- Là nền cho phân quyền (S07), state machine (S04), frontend theo role (S08).

## B. Overview diagram

### S01-D1 — Product map
```mermaid
mindmap
  root((QFlow Product))
    Problem
      Oversell bán trùng
      Sập hệ thống lúc mở bán
      Không công bằng vì bot và F5
      Giữ chỗ vô thời hạn
      BTC nhỏ thiếu công cụ
    Solution
      Waiting room công bằng
      Reservation có TTL
      Saga checkout
      Vé QR ký số và check-in idempotent
      Dashboard cho Organizer
    Target users
      Người tham dự
      Ban tổ chức nhỏ
      Staff tại cổng
      Ban kiểm duyệt nền tảng
    Roles
      GUEST
      ATTENDEE
      ORGANIZER
      EVENT_STAFF
      MODERATOR
      ADMIN
      SYSTEM
```

## C. Detailed diagrams

### S01-D2 — Vấn đề → giải pháp
> **ĐỀ XUẤT BỔ SUNG:** source liệt kê vấn đề và giải pháp thành hai danh sách riêng; các mũi tên ánh xạ dưới đây là suy luận từ nội dung, không phải bảng ánh xạ chính thức của source.
```mermaid
flowchart LR
  P1["1 Oversell / bán trùng"] --> S2["Reservation có TTL"]
  P1 --> S3["Saga checkout"]
  P2["2 Sập lúc mở bán"] --> S1["Waiting room"]
  P3["3 Không công bằng"] --> S1
  P4["4 Giữ chỗ vô thời hạn"] --> S2
  P5["5 BTC nhỏ thiếu công cụ"] --> S4["Vé QR ký số + check-in"]
  P5 --> S5["Dashboard Organizer"]
```

### S01-D3 — Role và trách nhiệm
```mermaid
flowchart LR
  GUEST["GUEST"] -->|"đọc"| PUB["Catalog đã PUBLISHED"]
  ATT["ATTENDEE"] -->|"join waiting room, tạo/xem/hủy"| MINE["Order và vé của chính mình"]
  ORG["ORGANIZER"] -->|"CRUD chỉ sự kiện mình tạo"| OWN["Sự kiện và ticket tier của mình"]
  ORG -->|"submit duyệt"| REV["Hàng chờ duyệt"]
  ORG -->|"gán staff"| STF["Event staff"]
  ORG -->|"xem báo cáo, yêu cầu hủy/hoàn"| RPT["Báo cáo sự kiện của mình"]
  STAFF["EVENT_STAFF"] -->|"check-in chỉ sự kiện được gán"| CHK["Check-in vé"]
  MOD["MODERATOR"] -->|"duyệt/từ chối kèm lý do, gỡ vi phạm"| REV
  ADM["ADMIN"] -->|"verify organizer, quản lý user"| USR["User và organizer profile"]
  ADM -->|"xem"| AUD["Audit log"]
  ADM -->|"hoàn tiền thủ công"| RFD["Refund"]
  ADM -->|"cấu hình rate limit và waiting room toàn cục"| CFG["Cấu hình nền tảng"]
  SYS["SYSTEM service account"] -->|"expire hold, reconcile order kẹt, replay DLQ<br/>dùng machine token riêng"| JOBS["Worker nội bộ"]
```

### S01-D4 — Separation of duties (những điều bị cấm)
```mermaid
flowchart TB
  ORG["ORGANIZER"] -.->|"KHÔNG"| X1["Tự duyệt sự kiện của mình"]
  MOD["MODERATOR"] -.->|"KHÔNG"| X2["Động vào tiền"]
  STAFF["EVENT_STAFF"] -.->|"KHÔNG"| X3["Đọc dữ liệu thanh toán"]
  SYS["SYSTEM"] -.->|"KHÔNG"| X4["Dùng token của người"]
  X1 --> W["Mục đích: separation of duties<br/>không tạo role chỉ để cho nhiều"]
  X2 --> W
  X3 --> W
  X4 --> W
```

## D. Giải thích sơ đồ
- **D1** gom 4 khối của mục 1 và danh sách role.
- **D2** cho thấy vì sao có waiting room (sập + không công bằng) và vì sao hold phải có TTL (giữ chỗ vô thời hạn). Một giải pháp có thể phục vụ nhiều vấn đề.
- **D3** thể hiện "ai làm được gì trên tài nguyên nào". Cạnh ghi đúng ràng buộc sở hữu: ATTENDEE chỉ với dữ liệu **của mình**, ORGANIZER chỉ với sự kiện **mình tạo**, STAFF chỉ với sự kiện **được gán**.
- **D4** là 4 điều cấm source nêu: người tạo nội dung không tự duyệt, người duyệt không đụng tiền, người ở cổng không thấy dữ liệu tiền, worker nội bộ không mượn danh người dùng.

## E. Những điều phải nhớ
- Mỗi role phải có trách nhiệm nghiệp vụ thật; không thêm role cho đẹp.
- Quyền "chỉ của mình / chỉ event mình tạo / chỉ event được gán" phải kiểm ở **service** (ownership), không chỉ ở gateway (xem S07).
- **SOURCE CHƯA QUY ĐỊNH:** quy tắc kết hợp khi một user có nhiều role (bảng `user_roles` cho phép nhưng source không nêu); GUEST không phải entity trong DB.

## F. Liên kết
- Đầu vào: S00. Đầu ra: S04 (ai chuyển state), S07 (RBAC), S08 (trang theo role), Phase 1.

## G. Checklist
- [x] 5 vấn đề  - [x] 5 giải pháp  - [x] 4 nhóm user  - [x] 7 role + trách nhiệm + quyền chính  - [x] separation of duties
