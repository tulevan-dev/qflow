# Performance journal

## P0-T09 · CI ngày 26/11/2026

[PR #1](https://github.com/tulevan-dev/qflow/pull/1) vào `develop` có ba lượt chạy xanh trên cùng commit `1d542948d052c9f4c7505e432f8107008c75b8d8`. Workflow dùng `ubuntu-latest`, Temurin 21 và lệnh `bash ./mvnw -B spotless:check verify`. Thời lượng là chênh lệch giữa thời điểm bắt đầu và kết thúc do GitHub Jobs API trả về.

| Run ID / attempt | Runner | Job build-and-test | Bước Build and test | Bằng chứng cache |
| --- | --- | ---: | ---: | --- |
| [45842434697 / 1](https://github.com/tulevan-dev/qflow/actions/runs/45842434697/attempts/1) | GitHub Actions 1000000000 | 45 giây | 35 giây | Cache Maven của PR được tạo ở cuối job. |
| [45842434697 / 2](https://github.com/tulevan-dev/qflow/actions/runs/45842434697/attempts/2) | GitHub Actions 1000000001 | 32 giây | 26 giây | Chưa xác nhận giá trị exact-hit của lượt này. |
| [45842434697 / 3](https://github.com/tulevan-dev/qflow/actions/runs/45842434697/attempts/3) | GitHub Actions 1000000002 | 34 giây | 25 giây | Cả hai cache Maven được truy cập đúng lúc bước setup Java chạy. |

[GitHub Actions Cache API](https://api.github.com/repos/tulevan-dev/qflow/actions/caches?ref=refs%2Fpull%2F1%2Fmerge) ghi nhận cache dependency `11159521477` và cache wrapper `11159520463`, cùng thuộc `refs/pull/1/merge`. Hai cache được truy cập lần cuối lúc 21:58:09 (UTC+7), trùng với thời điểm lượt 3 bắt đầu bước setup Java. Bước Maven ở lượt 3 ngắn hơn lượt 1 10 giây (~29%). Các lượt chạy dùng runner khác nhau, nên số đo phản ánh thời lượng quan sát được.

Ngày 27/11/2026, lệnh `.\mvnw.cmd -B spotless:check verify` trên Windows với JDK 21 chạy qua 19 test. Trong bản clone tạm, chạy lại với `-o` và một assertion cố tình fail trong `HelloApplicationTests.contextLoads` cho kết quả `BUILD FAILURE` và exit code 1. Test tạm đã được khôi phục. Minh chứng nghiệm thu còn lại là một lượt GitHub Actions đỏ do assertion của test.
