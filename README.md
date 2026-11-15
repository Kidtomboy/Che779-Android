<div align="center">

# 📱 Che779 Android

**Chạy game J2ME (`.jar`) ngay trên điện thoại — nhiều game cùng lúc, mỗi game một tab.**

Dựa trên J2ME Loader, thêm lớp vỏ Che779: đa tab, chế độ lưới, bong bóng nổi, proxy theo tab và tiết kiệm pin.
Cài một file APK là dùng, không cần Termux hay cấu hình gì thêm.

[![Phiên bản](https://img.shields.io/github/v/release/Kidtomboy/Che779-Android?label=phi%C3%AAn%20b%E1%BA%A3n&color=blue)](../../releases/latest)
[![Lượt tải](https://img.shields.io/github/downloads/Kidtomboy/Che779-Android/total?label=l%C6%B0%E1%BB%A3t%20t%E1%BA%A3i&color=brightgreen)](../../releases)
[![CI](https://img.shields.io/github/actions/workflow/status/Kidtomboy/Che779-Android/build-apk.yml?branch=main&label=CI)](../../actions)
[![Android 5.0+](https://img.shields.io/badge/Android-5.0%2B-3DDC84?logo=android&logoColor=white)](#-yêu-cầu-hệ-thống)
[![Giấy phép](https://img.shields.io/badge/giấy%20phép-Apache%202.0-orange)](LICENSE)

[Tải về](#-tải-về) · [Tính năng](#-tính-năng) · [Bắt đầu nhanh](#-bắt-đầu-nhanh) · [Hướng dẫn](#-hướng-dẫn-sử-dụng) · [Bản clone](#-tạo-bản-clone) · [Hỏi đáp](#-hỏi-đáp-và-xử-lý-lỗi) · [Dành cho dev](#-dành-cho-nhà-phát-triển) · [Tài liệu](#-tài-liệu) · [Ghi công](#-ghi-công-và-giấy-phép)

</div>

---

## 📥 Tải về

Vào mục **[Releases](../../releases/latest)** và tải file:

| File | Dùng để |
|---|---|
| `Che779-Android-<phiên bản>.apk` | Cài ứng dụng lên điện thoại Android |
| `SHA256SUMS.txt` | Kiểm tra file tải về có nguyên vẹn không |

Mục Releases còn có bản **"thử mới nhất (dev, tự động)"** — cập nhật mỗi khi mã nguồn thay đổi, có thể chưa ổn định. Muốn dùng ổn định, hãy tải bản `vX.Y.Z`.

> ⚠️ APK được ký bằng **khóa thử nghiệm công khai** của dự án để các bản cài đè lên nhau được. Đây không phải khóa phát hành riêng: hãy chỉ tải từ Releases của repo này. Khi cập nhật, giữ nguyên tên gói và chữ ký của bản đang cài.

---

## 🌐 Hệ sinh thái Che779

Che779 gồm 4 dự án, mỗi dự án một repo. Bạn đang xem **Project 4**.

| # | Dự án | Chạy trên | Dùng khi nào |
|:-:|---|---|---|
| 1 | [**Che779-Emulator**](https://github.com/Kidtomboy/Che779-Emulator) | Windows, Linux | Mỗi tab một cửa sổ, điều khiển bằng Remoter |
| 2 | [**Che779-EmulatorV2**](https://github.com/Kidtomboy/Che779-EmulatorV2) | Windows / Java 8+ | Nhiều tab trong một cửa sổ, một tiến trình |
| 3 | [**Che779-Linux**](https://github.com/Kidtomboy/Che779-Linux) | Linux và Termux | Một lệnh cài và chạy trên Linux / Android (Termux) |
| 4 | **Che779-Android** ← *bạn đang ở đây* | Android | Chạy trực tiếp trên điện thoại bằng APK |

> 📌 Tên `Che779-Emulator` từng là tên của repo này. Nay bản Windows/Linux dùng tên đó; bản Android là **Che779-Android**. Link tải APK cũ (`…/Che779-Emulator.apk`) không còn dùng được — hãy tải từ [Releases](../../releases/latest).

---

## ✨ Tính năng

| | Tính năng | Mô tả |
|:-:|---|---|
| 🗂️ | **Đa tab** | Mỗi game là một phiên riêng trong cùng một ứng dụng; dữ liệu, mạng và hiển thị tách theo tab |
| 🔢 | **Số tab cố định** | Đóng tab 3 rồi mở lại vẫn là tab 3; dữ liệu tài khoản theo số tab, không trùng nhau |
| 🔲 | **Chế độ lưới** | Xem nhiều game cùng lúc dạng ô, chuyển qua lại giữa danh sách và lưới |
| 💬 | **Bong bóng nổi** | Thu game thành bong bóng kéo được; chạm để mở cửa sổ nổi (đổi cỡ, thu nhỏ); kéo vào ✕ để tắt, chạm đúp để mở ứng dụng |
| ⌨️ | **Bàn phím ảo kiểu J2ME** | Bàn phím 12 phím dạng điện thoại, dùng ở chế độ lưới và trong cửa sổ nổi |
| 🔁 | **Đồng bộ phím** | Chọn các tab cùng nhận một phím bấm (tick từng tab) |
| 🌐 | **Proxy theo tab** | HTTP / SOCKS5 riêng từng tab, gán hàng loạt từ danh sách |
| 🔀 | **Chuyển hướng IP** | Chuyển hướng địa chỉ máy chủ theo từng tab |
| 🔋 | **Tiết kiệm pin** | 4 chế độ: Bình thường, Tiết kiệm, Siêu tiết kiệm, Headless; ô Cài đặt nhanh để đổi |
| 🧹 | **Quản lý bộ nhớ** | Treo (ngừng vẽ), ngủ màn hình, tự dọn khi RAM thấp, cảnh báo |
| 🧬 | **Bản clone** | Tạo thêm bản cài độc lập với tên gói, tên app và thư mục dữ liệu riêng |

> Các tính năng chính (đa tab, lưới, bong bóng, treo/ẩn/ngủ, proxy theo tab, chuyển hướng IP) đã được chủ dự án thử trên máy thật. Những tính năng còn lại chưa có xác nhận — xem danh sách ở [`CHANGELOG.md`](CHANGELOG.md).
>
> Không giới hạn số tab, nhưng RAM và pin của máy mới là giới hạn thật. Nhiều tab cùng một địa chỉ IP vẫn có thể bị máy chủ game chặn; dùng proxy riêng cho từng tab nếu cần.

---

## 🖥️ Yêu cầu hệ thống

| | Yêu cầu |
|---|---|
| Android | **5.0 trở lên** |
| Bộ nhớ | Tùy số tab và game; mỗi tab chạy một game nên nhiều tab sẽ tốn RAM và pin |
| Quyền | **Hiển thị trên ứng dụng khác** (chỉ cần nếu dùng bong bóng nổi) |

---

## 🚀 Bắt đầu nhanh

1. Tải `Che779-Android-<phiên bản>.apk` ở [Releases](../../releases/latest).
2. Cho phép cài ứng dụng từ nguồn ngoài Play Store khi Android hỏi, rồi cài.
3. Mở ứng dụng, bấm nút chọn game và chọn file `.jar`.

---

## 📖 Hướng dẫn sử dụng

1. **Thêm game:** bấm nút chọn game trên thanh tab và chọn file `.jar`.
2. **Thêm tab:** bấm **＋** (nhân bản tab 1; chạm giữ để chọn game khác). Chạm giữ vào tên tab để đổi tên.
3. **Xem lưới:** đổi sang chế độ lưới để thấy nhiều game cùng lúc; chạm một ô để chọn tab.
4. **Bong bóng:** bật bong bóng nổi để vừa chơi vừa dùng ứng dụng khác. Một số hãng máy giấu mục cấp quyền "hiển thị trên ứng dụng khác" khá sâu trong Cài đặt.
5. **Proxy / IP:** vào menu để gán proxy hoặc chuyển hướng IP cho từng tab hoặc cả nhóm.
6. **Tiết kiệm pin:** đổi chế độ pin ở menu hoặc ô Cài đặt nhanh.
7. **Thoát:** chọn *Thoát hết* hoặc *Đóng tab này*.

---

## ⚙️ Cấu hình và nơi lưu dữ liệu

| | |
|---|---|
| Gói mặc định | `com.che779.emulator` |
| Thư mục dữ liệu | `J2ME-Che779` trong bộ nhớ máy (Android 10 trở xuống); Android 11 trở lên nằm trong `Android/data/<tên gói>/files` |
| Nhật ký chẩn đoán | `che779-diag.txt` (xoay vòng, tối đa 3 MiB mỗi thế hệ) — gửi file này khi báo lỗi |

Mỗi tab có dữ liệu riêng; bản clone có thư mục dữ liệu tách hẳn bản gốc.

---

## 🧬 Tạo bản clone

Muốn có thêm một bản cài độc lập (tên gói, tên ứng dụng và dữ liệu riêng)? Vào **Actions → Build clone APK → Run workflow** và điền ba ô:

| Ô | Quy tắc | Ví dụ |
|---|---|---|
| `app_id` | Tên gói nhiều đoạn, mỗi đoạn bắt đầu bằng chữ | `com.che779.emulatorv2` |
| `app_name` | 1–40 ký tự: chữ, số, dấu cách, `._()-` | `Che779 Emulator V2` |
| `data_dir` | 1–64 ký tự chữ/số/`.`/`_`/`-`, không chứa `..` | `J2ME-Che779v2` |

Bản clone ra ở mục Releases với thẻ `clone-<tên gói>`. Mỗi bản clone có dữ liệu riêng nên cài trùng game ở hai bản sẽ tốn dung lượng gấp đôi.

---

## 🧩 Cách hoạt động

```mermaid
flowchart TD
    A[MicroActivity<br/>giao diện và vòng đời tab] --> S[Session<br/>trạng thái từng tab]
    S --> L[MicroLoader · MidletThread · AppClassLoader]
    S --> N[NetConfig · NetHooks<br/>proxy và chuyển hướng IP]
    A --> U[TabBar · GridPanel · GridBar]
    A --> F[FloatingController · BubbleService]
    A --> D[Che779Diag · DiagnosticLogStore]
```

Phần lõi J2ME Loader lo các API MIDP/CLDC; Che779 thêm lớp vỏ đa phiên bao quanh. Mỗi tab là một `Session` có class loader, dữ liệu và cấu hình mạng riêng. Chi tiết: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

---

## ❓ Hỏi đáp và xử lý lỗi

| Bạn gặp | Làm gì |
|---|---|
| Không cài được APK | Cho phép cài từ nguồn ngoài Play Store. Nếu đang cài đè, bản cũ phải cùng tên gói và cùng chữ ký. |
| Không bật được bong bóng nổi | Cấp quyền **hiển thị trên ứng dụng khác** cho ứng dụng trong Cài đặt máy. |
| Bản ghép nhiều game con (x6–x12) không vào được game con | Đây là lỗi đã biết, chưa sửa. Xem phần dưới. |
| Bàn phím J2ME gốc thụt xuống sau khi thoát chế độ lưới | Lỗi đã biết. Dùng mục menu **Sửa bàn phím ảo** hoặc bàn phím ảo của ứng dụng. |
| Game không vào được mạng | Thử gán proxy hoặc chuyển hướng IP cho tab. Lỗi mạng còn có thể do chính máy chủ game. |
| Muốn báo lỗi | Mở [Issue](../../issues/new/choose), ghi phiên bản, tên máy, Android và gửi `che779-diag.txt`. Xóa mật khẩu proxy khỏi log. |

---

## ⚠️ Giới hạn và vấn đề đã biết

- **Bản ghép nhiều game con** (kiểu x6–x12) hiện **chưa vào được game con**. Chưa sửa; chỉ sửa khi có yêu cầu.
- **Bàn phím J2ME gốc có thể thụt xuống** sau khi thoát lưới; đang che bằng bàn phím ảo mới.
- Bàn phím ảo mới chưa gõ chữ được vào ô nhập văn bản.
- Dọn luồng khi đóng tab dựa trên đoán theo tên lớp; hai game trùng tên lớp có thể ảnh hưởng nhau.
- Chưa tuyên bố tương thích mọi máy hoặc mọi game; quyền hiển thị trên ứng dụng khác thay đổi theo hãng.
- Một lần build xanh trên CI không thay cho việc thử trên máy thật.

Danh sách đầy đủ: [`docs/DEBUG_LOG.md`](docs/DEBUG_LOG.md). Phần nào đã kiểm chứng: [`docs/TESTING.md`](docs/TESTING.md).

---

## 🛠️ Dành cho nhà phát triển

<details>
<summary><b>Môi trường và lệnh build</b></summary>

JDK 21 (Temurin trên CI) · Android SDK Platform 34 · Android NDK `25.1.8937393` · Gradle Wrapper 8.10.2 (Android Gradle Plugin 8.7.3).

```bash
./gradlew testDevDebugUnitTest assembleDevDebug
```

Windows: `gradlew.bat testDevDebugUnitTest assembleDevDebug`. APK nằm ở `app/build/outputs/apk/dev/debug/`. Tải mã nguồn không có `.git` vẫn cấu hình được: số build lấy từ `GITHUB_RUN_NUMBER`, nếu không có thì dùng `1`.

</details>

<details>
<summary><b>Ký bản release</b></summary>

Bản `release` **không** tự dùng khóa thử nghiệm. Muốn ký, cấp đủ bốn biến môi trường trong môi trường build tin cậy: `CHE779_RELEASE_STORE_FILE` · `CHE779_RELEASE_STORE_PASSWORD` · `CHE779_RELEASE_KEY_ALIAS` · `CHE779_RELEASE_KEY_PASSWORD`. Không commit keystore hay mật khẩu. Thiếu các biến này thì release ra dạng chưa ký. Đừng đổi khóa của ứng dụng đã phát hành nếu còn cần cập nhật cài đè.

</details>

<details>
<summary><b>Phát hành</b></summary>

Gắn thẻ `vX.Y.Z` → workflow **Phát hành** kiểm thử, build, tạo `Che779-Android-<phiên bản>.apk` và `SHA256SUMS.txt` (mã băm tính sau cùng, trên đúng file phát hành). Mỗi lần có thay đổi trên `main`, workflow **Build Che779 APK** cập nhật bản thử `latest`.

</details>

---

## 🗂️ Cấu trúc repo

```
Che779-Android/
├─ README.md  CONTRIBUTING.md  SECURITY.md  LICENSE  CHANGELOG.md  VERSION
├─ app/            Ứng dụng Android (lớp vỏ Che779 ở javax/microedition/shell)
├─ dexlib/         Module Gradle phụ (thư viện nội bộ mà app phụ thuộc)
├─ gradle/  build.gradle  settings.gradle  gradlew*   ← Gradle bắt buộc nằm ở gốc
├─ docs/           Tài liệu kỹ thuật (có mục lục)
└─ .github/        Workflow build thử, Phát hành, Clone; mẫu Issue và Pull Request
```

---

## 📚 Tài liệu

[Mục lục](docs/README.md) · [Trạng thái](docs/PROJECT_STATE.md) · [Kiến trúc](docs/ARCHITECTURE.md) · [Cài đặt và build](docs/BUILD_AND_RUN.md) · [Kiểm thử](docs/TESTING.md) · [Lỗi đã biết](docs/DEBUG_LOG.md) · [Quyết định](docs/DECISIONS.md) · [Lộ trình](docs/ROADMAP.md) · [Clone và biến thể](docs/VARIANTS_AND_CLONES.md) · [Nguồn gốc mã](docs/SOURCE_INVENTORY.md) · [Bàn giao](docs/AI_HANDOFF.md)

---

## 🤝 Đóng góp và bảo mật

Muốn góp sức? Đọc [`CONTRIBUTING.md`](CONTRIBUTING.md). Tìm thấy lỗ hổng bảo mật? Xem [`SECURITY.md`](SECURITY.md) và báo riêng, đừng đăng công khai.

---

## 🙏 Ghi công và giấy phép

Che779 Android kế thừa **[J2ME Loader](https://github.com/nikita36078/J2ME-Loader)** của Nikita Shakarun và các cộng tác viên (Apache License 2.0). Một số ý tưởng giao diện và tính năng đến từ các bản mod cộng đồng: **Hùng Hero** (J2ME Loader v10) và **NST Tiến** (J2ME_NST). Giữ nguyên các thông báo bản quyền khi phân phối lại; xem [`LICENSE`](LICENSE).

<div align="center">

Làm bởi **Cherry** · [Báo lỗi](../../issues/new/choose) · [Đóng góp](CONTRIBUTING.md) · [Bảo mật](SECURITY.md)

</div>
