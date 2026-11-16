# Termux — Che779 Emulator V2

Script một file chạy **Emulator V2** trong Termux (Android). Termux chạy qua `proot-distro` (Ubuntu) + Java + Openbox + Termux:X11.

## Dùng
1. Cài Termux và Termux:X11.
2. Tải `che779-termux.sh` ở Releases → `bash che779-termux.sh` (tùy chọn: `-r MB`, `-g TÊN_GAME`, `--net`, `--doctor`, `--reset`, `-V`, `-h`).
3. Game: `Download/Che779Games`; dữ liệu: `Download/Che779Data`.

## Cách làm ra file
- `termux/che779-termux.sh`: **bản nguồn** (có `@VERSION@`, `@JAR_SHA256@`, `@JAR_NAME@`, chưa có jar).
- `termux/build-termux.sh <jar> <tên-jar> <file-ra>`: tính SHA-256 từ chính jar, điền vào script, nối jar base64.
- `.github/scripts/fetch-emulator.sh v2`: lấy Che779-EmulatorV2 đúng commit trong `termux/EMULATOR_V2_REF` và build jar.
- `bash tests/test-termux.sh <jar>`: kiểm thử (SHA-256, từ chối jar sai, không cần `unzip`; giả lập Termux bằng `CHE_FORCE_OS=termux`).

## Đã thử / chưa thử
- **Đã thử:** Cherry thử trên Termux thật (bản script cũ ở Che779-Linux): chạy được, chơi được game.
- **Đã thử trong môi trường Claude:** ghép script, kiểm thử SHA-256, dry-run.
- **Chưa thử:** bản 1.3.0 sau khi tách khỏi Che779-Linux chưa được chạy lại trên Termux thật.
