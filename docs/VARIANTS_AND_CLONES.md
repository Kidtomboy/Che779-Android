# Biến thể build và bản clone

## Bản clone

Vào **Actions → Build clone APK → Run workflow** và điền:

| Ô | Quy tắc | Ví dụ |
|---|---|---|
| `app_id` | Tên gói nhiều đoạn, mỗi đoạn bắt đầu bằng chữ | `com.che779.emulatorv2` |
| `app_name` | 1–40 ký tự: chữ, số, dấu cách, `._()-` | `Che779 Emulator V2` |
| `data_dir` | 1–64 ký tự chữ/số/`.`/`_`/`-`, không chứa `..` | `J2ME-Che779v2` |

Bản clone ra ở mục Releases với thẻ `clone-<tên gói>`. Tham số được kiểm tra cả ở workflow lẫn Gradle. Mỗi bản clone có dữ liệu riêng.

## Biến thể build

- `devDebug`: bản thử và bản phát hành hiện tại, ký bằng khóa thử nghiệm của repo để cài đè được.
- `playDebug`, `openDebug`, `fdroidDebug`: biến thể gốc của J2ME Loader.
- `release`: chưa ký trừ khi cấp đủ bốn biến `CHE779_RELEASE_*`.
- `midlet`: biến thể đặc biệt tạo bản Android riêng cho một MIDlet (tên gói, phiên bản lấy từ manifest MIDlet).
