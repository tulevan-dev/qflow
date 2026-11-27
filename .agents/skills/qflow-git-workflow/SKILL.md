---
name: qflow-git-workflow
description: Chuẩn bị branch, commit, pull request, release hoặc hotfix cho QFlow và xác minh nhánh đích, tag, version và CI.
---

# Git workflow QFlow

- Kiểm tra worktree, remote, nhánh hiện tại và nhánh đích. [README](../../../README.md) dùng `develop` để bắt đầu và các PR Phase 0 đã vào `develop`. [CI](../../../.github/workflows/ci.yml) chạy cho PR vào `main`, `develop`, `release/**` và khi push lên `main`. Xác minh đích theo yêu cầu trước khi tạo PR.
- [Tài liệu workflow](../../../docs/diagrams/20-workflow-docs-adr/S20-workflow-docs-adr.md) minh họa nhánh ngắn và Conventional Commits, ví dụ `fix(inventory): prevent negative stock`. Khi Codex được yêu cầu tạo nhánh, dùng prefix `codex/` nếu người dùng chưa chỉ định tên khác.
- Khi chuẩn bị release, kiểm tra version Maven, tag hiện có, diff kể từ bản trước và kết quả CI trên commit dự kiến. Tài liệu minh họa tag `vX.Y.Z`. Với hotfix, xác nhận commit hoặc tag đang triển khai, nhánh nhận hotfix và nhánh cần đồng bộ theo quy trình release đã thống nhất. Dùng phạm vi thao tác Git được yêu cầu.
