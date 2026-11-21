# BÀN GIAO CLAUDE — RÀ SOÁT VÀ CHUẨN HÓA TOÀN BỘ GITHUB ACTIONS WORKFLOWS

Repository: https://github.com/Kidtomboy/Che779-Android

Mục tiêu: Claude rà soát và chỉnh sửa toàn bộ GitHub Actions workflows để luồng build/test/clone/release rõ ràng, an toàn, không tự xóa tag hoặc release, không tạo Release V2 cuốn chiếu ngoài ý muốn. Đây là công việc workflow/CI-CD; không tự ý sửa mã nguồn ứng dụng.

## 1. Trạng thái V2 đã được xử lý — không làm lại

Release V2 đã được khôi phục thành công. Không được chạy lại migration, build lại APK V2, ghi đè asset, di chuyển tag hoặc xóa thêm release/tag.

- Release: https://github.com/Kidtomboy/Che779-Android/releases/tag/dev-2.0.0
- Workflow thành công: https://github.com/Kidtomboy/Che779-Android/actions/runs/44937269867
- Source commit dùng để khôi phục: `36b28699868c2e37d9a2a657b328c11134f835c3`
- Tag `dev-2.0.0` vẫn trỏ commit `39a3c8e8e885e2701231e7877087b51dcfc1e1c9`; tuyệt đối không di chuyển tag này.
- Asset `Che779-AndroidV2-2.0.0.apk`: 16,398,975 bytes.
- SHA-256 APK: `38b0a8ba7d67a5a02bb6a95af4b1d01fe9f9954e73287c518fb2208ac51391a8`.
- Asset `SHA256SUMS.txt` đã có trong Release.
- Release `build-v2` đã được dọn khỏi danh sách Release; không tạo lại.
- Các Release `buildlog`, `v1.3.1`, `v1.3.0`, `v1.2.0` đã được xác nhận còn tồn tại tại lần kiểm tra gần nhất.

Lưu ý quan trọng: APK trong Release có SHA-256 trùng chính xác với APK V2 được ghi nhận trước khi mất Release nguồn. Không build lại hoặc thay thế bằng APK khác.

## 2. Phạm vi workflows đã kiểm tra

Nhánh `main`:
1. `.github/workflows/build-apk.yml` — Build bản dev, chạy unit tests, xuất artifact và cập nhật Release nhật ký build.
2. `.github/workflows/clone.yml` — Tạo bản clone theo tham số.
3. `.github/workflows/release.yml` — Build và phát hành bản `v*`, hoặc cập nhật Release đã có.
4. `.github/workflows/migrate-v2-release.yml` — Workflow khôi phục V2 một lần; nhiệm vụ đã hoàn tất.

Nhánh `build-v2`:
5. `.github/workflows/build-v2.yml` — Build/phát hành Android V2 theo tag `dev-*`.

Không giả định rằng file cùng tên ở các branch có nội dung giống nhau. Kiểm tra và sửa đúng branch, đúng blob SHA; không ghi đè nhầm.

## 3. Yêu cầu chuẩn hóa chung

Áp dụng cho tất cả workflow:

- Dùng `set -euo pipefail` cho shell script, trừ chỗ có chủ đích xử lý lỗi và có giải thích.
- Chỉ cấp quyền tối thiểu cần thiết. Workflow chỉ đọc/build thì không cấp `contents: write`.
- Các workflow ghi Release phải khai báo `permissions: contents: write` rõ ràng.
- Thêm `concurrency` phù hợp để tránh hai lần chạy đồng thời cùng sửa một Release/tag.
- Không dùng `gh release delete ... --cleanup-tag` trong luồng chạy thường xuyên.
- Không xóa rồi tạo lại tag để cập nhật asset. Tag phiên bản là bất biến.
- Khi Release đã tồn tại: dùng `gh release edit` và `gh release upload --clobber` cho đúng asset cần cập nhật.
- Khi Release chưa tồn tại: chỉ tạo nếu workflow có đúng nhiệm vụ phát hành và đã xác thực tag/commit.
- Tạo checksum sau khi đã thu thập toàn bộ asset cuối cùng; checksum phải tham chiếu đúng tên file phát hành.
- Sau khi upload, tải asset từ Release đích về và xác minh checksum/nội dung trước khi báo thành công.
- Dùng artifact cho file build trung gian/log; không biến artifact thành Release công khai ngoài ý định.
- Không dùng wildcard copy thiếu kiểm soát. Kiểm tra có đúng số APK mong muốn trước khi copy/upload.
- Không in secret/token vào log; truyền qua `env`, không nội suy secret trực tiếp trong shell.
- Giữ nguyên tên các Release/tag đang được người dùng yêu cầu bảo tồn.
- Không sửa Java/Kotlin/XML/Gradle app source, logic ứng dụng, package ID hay chức năng app trong nhiệm vụ này.

## 4. Việc cần làm theo từng workflow

### A. `build-apk.yml` — build dev và nhật ký build

Hiện trạng:
- Chạy khi push `main` và workflow_dispatch.
- Chạy `testDevDebugUnitTest assembleDevDebug`.
- Đưa APK lên Actions artifact.
- Bước publish log đang xóa Release `buildlog` cùng tag rồi tạo lại mỗi lần chạy.

Yêu cầu sửa:
1. Giữ trigger push `main` và workflow_dispatch nếu vẫn phù hợp với quy trình dev.
2. Giữ unit test + build và upload artifact APK.
3. Bảo đảm bước log chạy được cả khi Gradle thất bại hoặc file `build.log` chưa tồn tại; tạo log dự phòng có thông tin lỗi.
4. Thay cơ chế xóa Release/tag `buildlog` bằng upsert an toàn:
   - Nếu Release `buildlog` chưa tồn tại: tạo Release prerelease/latest=false.
   - Nếu đã tồn tại: cập nhật notes/title và upload `build-tail.log` bằng `--clobber`.
   - Không xóa tag `buildlog`.
5. Nếu không thể bảo đảm cập nhật log mà không di chuyển tag, ưu tiên lưu log dưới Actions artifact và báo rõ giới hạn; không xóa tag.
6. Log không được làm che khuất kết quả build chính. Job build thất bại vẫn phải có conclusion failure dù bước publish log chạy thành công.

### B. `clone.yml` — tạo bản clone

Hiện trạng:
- Chạy thủ công với `app_id`, `app_name`, `data_dir`.
- Có kiểm tra input.
- Đang xóa Release/tag clone cũ trước khi tạo lại.

Yêu cầu sửa:
1. Giữ validate package ID, tên app và thư mục dữ liệu; bổ sung kiểm tra không rỗng, độ dài, ký tự an toàn.
2. Chạy unit tests trước build.
3. Kiểm tra APK đầu ra tồn tại và package ID bên trong APK đúng bằng `app_id` được nhập.
4. Tạo `SHA256SUMS.txt` cho APK.
5. Upload APK + checksum lên Actions artifact.
6. Với Release clone:
   - Tạo nếu chưa tồn tại.
   - Nếu đã tồn tại, cập nhật asset cùng tag ổn định bằng `--clobber`.
   - Không xóa Release/tag cũ.
   - Không đặt latest cho clone prerelease.
7. Tag clone phải được tạo từ commit workflow đang build khi tạo lần đầu. Khi cập nhật Release đã tồn tại, không force-move tag. Ghi rõ commit nguồn trong notes.
8. Nếu cần đổi package ID dẫn tới tag clone khác, coi đó là một clone mới, không xóa clone cũ.

### C. `release.yml` — phát hành phiên bản chính `v*`

Hiện trạng:
- Push tag `v*` sẽ build và phát hành.
- workflow_dispatch có input `update_tag` để cập nhật Release có sẵn.
- Build APK chính, clone APK, Termux V2; tạo thêm script V1 thử nghiệm dạng artifact.
- Đưa assets lên Release; cập nhật Release có sẵn bằng gh CLI.

Yêu cầu sửa:
1. Phân biệt rõ hai chế độ:
   - Phát hành mới từ tag `vX.Y.Z`.
   - Cập nhật asset của một Release/tag `vX.Y.Z` đã tồn tại.
2. Validate `update_tag` theo mẫu `v[0-9]+\.[0-9]+\.[0-9]+`; xác nhận Git tag và Release đích tồn tại trước khi build/upload.
3. Khi cập nhật tag đã tồn tại, checkout/build từ chính commit của tag đích, không build tùy tiện từ HEAD của `main`.
4. Kiểm tra giá trị file `VERSION` khớp phiên bản tag trước khi upload; nếu không khớp thì dừng, không ghi đè Release.
5. Không bao giờ di chuyển/xóa tag `v*`.
6. Giữ nguyên cách tạo APK chính, APK clone và Termux script theo thiết kế hiện tại; không tự ý đổi signing hoặc package.
7. Tạo SHA256SUMS sau cùng từ chính xác các asset được đưa lên Release.
8. Giữ artifact `termux-v1-test` riêng, không đưa script V1 thử nghiệm vào Release chính.
9. Với cập nhật Release đã có, upload asset trước, checksum sau; chỉ cập nhật notes/title sau khi asset và checksum được xác minh.
10. Không xóa Release hoặc asset ngoài danh sách asset được workflow sở hữu. Không xóa `build-tail.log` nếu chưa xác nhận nó thực sự nằm sai Release; log build thuộc Release `buildlog`.
11. Có concurrency theo tag để hai lần phát hành cùng phiên bản không ghi đè lẫn nhau.
12. Ghi rõ trong summary commit nguồn, tag, phiên bản, tên asset, kích thước và SHA-256.

Lưu ý: Release notes hiện nói APK ký bằng khóa thử nghiệm công khai. Không tự ý thay đổi signing trong đợt chuẩn hóa workflow này; chỉ giữ nguyên thông tin và báo rủi ro riêng nếu cần.

### D. `migrate-v2-release.yml` — migration một lần

Nhiệm vụ khôi phục đã thành công. Workflow hiện có trigger push theo path, nên sửa file này có thể vô tình chạy lại quá trình build/upload V2 và ghi đè asset.

Yêu cầu:
1. Không chạy lại migration.
2. Xóa workflow one-off này khỏi `.github/workflows` sau khi xác nhận nội dung bàn giao đã lưu; hoặc chuyển nó thành workflow manual-only có guard chặn chạy lại. Ưu tiên xóa file workflow để không có trigger tự động bất ngờ.
3. Không xóa Release `dev-2.0.0`, tag `dev-2.0.0`, asset APK hoặc SHA256SUMS.
4. Không thay thế APK hiện tại.

### E. `build-v2.yml` trên branch `build-v2` — phát hành V2 theo tag dev

Hiện trạng:
- Workflow nằm trên branch riêng `build-v2`.
- Trigger tag `dev-*`; workflow_dispatch có mode build/migrate.
- Nhánh migrate vẫn tham chiếu Release `build-v2` đã bị dọn.
- Không được tạo Release cuốn chiếu `build-v2`.

Yêu cầu sửa:
1. Loại bỏ hoàn toàn mode `migrate` và các bước tải/xóa Release `build-v2`.
2. Chỉ hỗ trợ build/phát hành tag `dev-*`.
3. Với push tag: xác định tag từ `GITHUB_REF_NAME`, xác minh tag trỏ đúng commit được checkout.
4. Với workflow_dispatch: bắt buộc nhập tag `dev-*`, fetch và checkout đúng commit của tag đó trước khi build; không lấy nhầm HEAD của branch.
5. Validate tag và version; kiểm tra package ID là `com.che779.emulatorv2`, versionName phù hợp metadata V2 và file APK đầu ra duy nhất.
6. Tạo APK tên `Che779-AndroidV2-<version>.apk` cùng SHA256SUMS.
7. Chỉ upload vào Release có tag tương ứng `dev-*`. Nếu Release chưa có thì tạo prerelease; nếu đã có thì cập nhật asset an toàn.
8. Không xóa/di chuyển tag, không đặt latest, không tạo Release tên `build-v2`.
9. Upload artifact Actions để có bản build dự phòng, nhưng không dùng artifact thay thế Release.
10. Có concurrency theo tag.
11. Không sửa tag `dev-2.0.0` hoặc ghi đè APK V2 hiện tại trong đợt chuẩn hóa. Nếu cần test workflow, dùng tag thử nghiệm riêng và được Cherry cho phép.

## 5. Phân công và branch

- Workflow chính `build-apk.yml`, `clone.yml`, `release.yml`, xử lý/xóa workflow migration: branch `main`.
- Workflow V2: branch `build-v2`.
- Không gộp nhầm thay đổi từ branch này sang branch kia.
- Tạo PR hoặc commit có mô tả rõ từng workflow; không gom thay đổi khó review.
- Không sửa repository khác.

## 6. Kiểm thử bắt buộc trước khi báo hoàn thành

Claude phải đưa bằng chứng thực tế, không chỉ đọc YAML:

1. Parse/lint toàn bộ YAML workflows.
2. Kiểm tra GitHub Actions nhận diện tất cả workflow, trigger và input.
3. Kiểm tra các shell script bằng `bash -n` hoặc shellcheck nếu có.
4. Chạy build/test workflow dev; xác nhận log và artifact.
5. Chạy clone trên thông số thử nghiệm riêng, không ghi đè clone người dùng đang dùng.
6. Kiểm tra release workflow bằng tag thử nghiệm riêng hoặc phương pháp dry-run không upload Release chính.
7. Kiểm tra V2 bằng tag thử nghiệm riêng, không ghi đè `dev-2.0.0`.
8. Kiểm tra chạy lặp workflow không xóa tag/Release và không tạo Release `build-v2`.
9. Xác minh các Release sau vẫn còn nguyên:
   - `dev-2.0.0`
   - `buildlog` (Nhật ký build)
   - `v1.3.1`
   - `v1.3.0`
   - `v1.2.0`
10. So sánh asset V2 cuối cùng với SHA-256 đã ghi ở Mục 1. Nếu khác, dừng và báo; không tự ý thay thế.

## 7. Ranh giới không được vượt qua

- Không sửa mã nguồn ứng dụng.
- Không build lại hoặc thay APK V2 đã được khôi phục.
- Không xóa/sửa tag `dev-2.0.0), `v1.3.1`, `v1.3.0`, `v1.2.0`.
- Không xóa Release phiên bản hiện có.
- Không tạo Release `build-v2` hoặc tên “Che779 Android V2 (bản dựng mới nhất)”.
- Không xóa Release `buildlog`; chỉ cập nhật an toàn nội dung nhật ký.
- Không tự tạo tag/release thử nghiệm có thể bị người dùng hiểu là bản chính thức.
- Không báo “đã kiểm thử” nếu chỉ lint YAML hoặc chỉ đọc workflow.

## 8. Báo cáo bàn giao lại cho Cherry

Trả lời bằng bảng:

| Workflow | Branch | Thay đổi | Commit/PR | Lint | Runtime test | Kết quả |
|---|---|---|---|---|---|---|

Kèm theo:
- Danh sách file đã sửa/xóa.
- Link từng workflow run.
- Kết quả test thực tế.
- Danh sách Release/tag sau cùng.
- Xác nhận SHA-256 V2 vẫn là giá trị đã nêu ở Mục 1.
- Những kiểm thử không thực hiện được và lý do.

Không được tuyên bố hoàn tất nếu còn workflow chưa rà soát hoặc còn lỗi runtime chưa giải thích.


---

## 9. Nhật ký thực hiện chuẩn hóa workflows — 2026-11-22

Đã cập nhật trực tiếp các workflow trên đúng branch, không tạo branch mới và không sửa mã nguồn ứng dụng:

- `.github/workflows/build-apk.yml` — thêm concurrency; bảo đảm log luôn được tạo kể cả khi Gradle fail; lưu log thành Actions artifact; cập nhật Release `buildlog` theo kiểu upsert, không xóa tag; kiểm tra đúng một APK và tạo checksum.
- `.github/workflows/clone.yml` — giữ kiểm tra input; chạy unit tests; xác minh package ID thực tế trong APK; tạo checksum và Actions artifact; cập nhật Release clone theo kiểu upsert, không xóa/di chuyển tag; tải ngược asset để so sánh.
- `.github/workflows/release.yml` — tách bước resolve tag và build; manual update chỉ chấp nhận tag `vX.Y.Z` có Release tồn tại; build từ commit tag bất biến; đối chiếu `VERSION`; kiểm tra số lượng APK và package clone; tạo checksum sau cùng; giữ V1 thử nghiệm ở Actions artifact; xác minh checksum sau upload trước khi cập nhật notes.
- `.github/workflows/migrate-v2-release.yml` — đã xóa workflow migration một lần sau khi nội dung bàn giao bảo toàn trạng thái V2; không xóa Release/tag/asset V2.
- `.github/workflows/build-v2.yml` trên branch `build-v2` — bỏ hoàn toàn mode migrate và mọi thao tác với Release `build-v2`; manual run phải nhập tag `dev-* `; checkout đúng commit tag; chặn tuyệt đối `dev-2.0.0`; xác minh package/version APK; tạo checksum/artifact; chỉ upsert Release đúng tag và xác minh asset sau upload.

Commit đã ghi nhận:
- main build-apk: `f5c42e6135e8a40f6082220b859e1823369ae3da`
- main clone: `87cedf57bdc8119fbbf49d313653f33309feaef6`
- main release: `93d7fd7529f7c0dad8e724580c9234d85052809b`
- main remove migration: `7e7a53d22c5b09a699042d0c1175d2076fa95bea`
- branch build-v2: `67cab8c9e6559fa4b8baa99abfafff7bdf97fe39`

**Kiểm thử runtime/lint:** các thay đổi đã được ghi vào GitHub; chưa được đánh dấu lint/runtime pass chỉ dựa trên việc cập nhật file. Phải đối chiếu các workflow run tự kích hoạt sau các commit trên, đọc job logs/artifacts và xác nhận các run mới nhất. Không dispatch clone/release/V2 trên tag thật để thử nghiệm vì có thể tạo/cập nhật Release. Không chạy V2 trên `dev-2.0.0`.

**Bất biến V2:** không chạy migration hoặc build lại V2 đã khôi phục. SHA-256 kỳ vọng của APK trong Release `dev-2.0.0` vẫn là `38b0a8ba7d67a5a02bb6a95af4b1d01fe9f9954e73287c518fb2208ac51391a8`; cần xác minh Release asset sau khi hoàn tất kiểm tra CI, không được thay asset nếu chưa được phép.

Bản ghi này chỉ phản ánh thay đổi đã commit; không được ghi “hoàn tất” cho đến khi lint và runtime CI được xác minh bằng kết quả GitHub Actions thực tế.
