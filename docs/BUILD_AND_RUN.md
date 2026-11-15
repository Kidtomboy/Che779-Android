# Cài đặt và build

## Cài đặt (người dùng)

1. Mở mục **Releases** và tải `Che779-Android-<phiên bản>.apk`; kiểm tra bằng `SHA256SUMS.txt`.
2. Cho phép cài từ nguồn ngoài Play Store khi Android hỏi.
3. Cấp quyền **hiển thị trên ứng dụng khác** nếu muốn dùng bong bóng nổi.

Cần Android 5.0 trở lên (`minSdk 21`, `targetSdk 29`, `compileSdk 34`). Thư mục dữ liệu: `J2ME-Che779` trong bộ nhớ máy (Android 10 trở xuống); Android 11 trở lên nằm trong `Android/data/<tên gói>/files`.

## Build (nhà phát triển)

Cần JDK 21, Android SDK Platform 34, NDK `25.1.8937393`, Gradle Wrapper 8.10.2 (Android Gradle Plugin 8.7.3).

```bash
./gradlew testDevDebugUnitTest assembleDevDebug
```

Windows: `gradlew.bat ...`. APK nằm ở `app/build/outputs/apk/dev/debug/`. Tải mã nguồn không có `.git` vẫn cấu hình được: số build lấy từ `GITHUB_RUN_NUMBER`, nếu không có thì dùng `1`.

## Ký bản release

Bản `release` **không** tự dùng khóa thử nghiệm. Muốn ký, cấp đủ bốn biến môi trường trong môi trường build tin cậy: `CHE779_RELEASE_STORE_FILE`, `CHE779_RELEASE_STORE_PASSWORD`, `CHE779_RELEASE_KEY_ALIAS`, `CHE779_RELEASE_KEY_PASSWORD`. Không commit keystore hay mật khẩu. Thiếu các biến này thì release ra dạng chưa ký. Đừng đổi khóa của ứng dụng đã phát hành nếu còn cần cập nhật cài đè.

## Phát hành tự động

- Mỗi lần có thay đổi trên `main`, workflow **Build Che779 APK** kiểm thử, build và cập nhật bản thử `latest` (đánh dấu pre-release).
- Gắn thẻ `vX.Y.Z` thì workflow **Phát hành** build và tạo `Che779-Android-<phiên bản>.apk` + `SHA256SUMS.txt` ở mục Releases.
