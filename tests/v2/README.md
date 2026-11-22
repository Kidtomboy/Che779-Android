# Bản V2 (thử nghiệm) — mã nguồn nằm trong `tests/v2/`

Nhánh `build-v2` được chuyển vào đây để không xung đột với `main`.
`overlay/` chứa **chỉ các tệp V2 khác bản chính** (cùng đường dẫn với gốc repo).
Workflow "Phát hành bản V2 (dev-*)" chép `overlay/` đè lên mã `main` rồi build
(gói `com.che779.emulatorv2`, tên "Che779 Emulator V2", phiên bản 2.0.1).

Sửa V2: sửa tệp trong `tests/v2/overlay/...`. Mã chung (không nằm trong overlay) lấy theo `main`.
