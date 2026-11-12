# Che779 Emulator — Android

Ứng dụng Android chạy game J2ME (`.jar`) trực tiếp trên điện thoại, kế thừa engine J2ME Loader 1.8.2 và bổ sung lớp vỏ Che779: đa phiên/tab, proxy và IP redirect theo tab, chế độ lưới, bộ điều khiển nổi và chẩn đoán.

- **Package mặc định:** `com.che779.emulator`
- **Version hiển thị:** `0.0.0.2-android` (bản CI `devDebug` có hậu tố `-dev-<build>`)
- **versionCode:** `103`
- **minSdk / targetSdk / compileSdk:** `21 / 29 / 34`
- **Trạng thái:** thử nghiệm; chưa tuyên bố tương thích mọi thiết bị hoặc mọi MIDlet.

## Tải bản thử nghiệm

1. Mở tab **Actions** của repository và chọn lần chạy **Build Che779 APK** mới nhất có trạng thái thành công.
2. Tải artifact **Che779-Emulator-APK**, giải nén nếu trình duyệt tải về dạng ZIP.
3. Cài `Che779-Emulator.apk` trên Android. Nếu cập nhật, cần giữ nguyên package và chữ ký của bản đang cài.

Workflow CI tạo bản `devDebug` được ký bằng **khóa thử nghiệm công khai** trong repository để người thử nghiệm có thể cài đè các bản dev. Khóa này không phải khóa phát hành an toàn; không dùng APK ký bằng khóa thử nghiệm làm bản phát hành chính thức.

## Dành cho người phát triển

### Môi trường

- JDK 21 (Temurin được dùng trên CI).
- Android SDK Platform 34, Android Build Tools tương thích với Android Gradle Plugin 8.7.3.
- Android NDK `25.1.8937393` và CMake/ndk-build theo cấu hình dự án.
- Gradle Wrapper 8.10.2; không cần cài Gradle toàn cục.

### Build và test

```bash
./gradlew testDevDebugUnitTest assembleDevDebug
```

Windows:

```bat
gradlew.bat testDevDebugUnitTest assembleDevDebug
```

APK nằm dưới `app/build/outputs/apk/dev/debug/`. Source archive không có `.git` vẫn cấu hình được; build number sẽ dùng `GITHUB_RUN_NUMBER` nếu có, nếu không dùng giá trị dự phòng `1`.

### Tạo bản clone

Workflow **Build clone APK** nhận ba tham số:

| Input | Quy tắc | Ví dụ |
|---|---|---|
| `app_id` | application ID dạng nhiều segment, mỗi segment bắt đầu bằng chữ | `com.che779.emulatorv2` |
| `app_name` | 1–40 ký tự ASCII chữ/số, dấu cách và `._()-` | `Che779 Emulator V2` |
| `data_dir` | 1–64 ký tự chữ/số/dấu `.`, `_`, `-`; không chứa `..` | `J2ME-Che779v2` |

Workflow validate input trước khi build và truyền input qua environment/argument riêng, không ghép trực tiếp vào mã shell. Clone có package và thư mục dữ liệu riêng; không đổi `data_dir` của bản cài hiện có nếu muốn giữ dữ liệu.

### Ký bản release

Bản `release` **không tự dùng khóa thử nghiệm**. Muốn ký release, cấp đủ bốn biến môi trường trong môi trường build tin cậy:

- `CHE779_RELEASE_STORE_FILE`
- `CHE779_RELEASE_STORE_PASSWORD`
- `CHE779_RELEASE_KEY_ALIAS`
- `CHE779_RELEASE_KEY_PASSWORD`

Không commit keystore hoặc mật khẩu vào Git. Nếu không cấu hình các biến này, Gradle tạo release unsigned; phải ký bằng khóa phát hành riêng trước khi phân phối. Không thay khóa của ứng dụng đã phát hành nếu cần cập nhật cài đè.

## Kiến trúc Che779

| Thành phần | Trách nhiệm |
|---|---|
| `shell/Session` | Ngữ cảnh dữ liệu và trạng thái cho từng tab |
| `shell/MicroActivity` | Activity Android, kết nối UI và vòng đời các tab |
| `shell/MicroLoader`, `MidletThread` | Nạp MIDlet và điều phối lifecycle theo Session |
| `shell/AppClassLoader` | Class loader riêng của từng game |
| `shell/NetConfig`, `org/microemu/cldc/NetHooks` | Proxy/redirect theo tab |
| `shell/FloatingController`, `BubbleService` | Cửa sổ game/bong bóng nổi |
| `shell/TabBar`, `GridPanel` | Điều hướng tab và chế độ lưới |
| `shell/Che779Diag`, `DiagnosticLogStore` | Log file có xoay vòng và dump luồng có giới hạn |

`MicroActivity` hiện vẫn là lớp lớn và là ứng viên refactor tiếp theo. Không nên tách sâu trong cùng lượt sửa nếu chưa có test instrumentation và ma trận kiểm thử thiết bị.

## Giấy phép và ghi công

Dự án kế thừa J2ME Loader của Nikita Shakarun và cộng tác viên; xem `LICENSE` và `NOTICE` (Apache License 2.0). Ý tưởng bong bóng/tiện ích được ghi công trong mã nguồn và tài liệu dự án. Giữ nguyên các thông báo bản quyền và giấy phép khi phân phối lại.

## Giới hạn xác minh

Build Android thực tế, cài APK, quyền overlay theo OEM, tương thích từng game, proxy HTTP/SOCKS5, IP redirect và chạy nhiều giờ trên thiết bị thật cần được kiểm thử trên thiết bị Android. Một lần build xanh không thay thế ma trận kiểm thử này.

## CHE779 maintenance follow-up — 2026-11-13

- `app/src/main/java/javax/microedition/shell/FloatingController.java`: if attaching the floating game window fails, the controller now attempts to restore the bubble so controls remain reachable.
- The Gradle wrapper executable bit is restored in this archive.

The Android APK was not built in the current environment: Gradle 8.10.2 could not be downloaded because `services.gradle.org` was not resolvable, and no Android SDK/NDK was configured. Session thread isolation, global proxy authentication and Android permission/cleartext policy still require dedicated review and device/integration tests.
