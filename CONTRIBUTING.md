# Đóng góp cho Che779 Android

Cảm ơn bạn đã quan tâm! Tài liệu này giúp bạn gửi thay đổi nhanh và ít phải sửa lại.

## Trước khi bắt đầu

1. Đọc `docs/PROJECT_STATE.md`, `docs/ARCHITECTURE.md` và `docs/DECISIONS.md` (có các quyết định sản phẩm cần giữ).
2. Lớp vỏ Che779 nằm ở `app/src/main/java/javax/microedition/shell/`; phần còn lại là J2ME Loader gốc.
3. Không đưa vào repo: file APK, keystore thật, mật khẩu, dữ liệu người dùng.

## Cách gửi thay đổi

1. Fork repo, tạo nhánh từ `main`.
2. Sửa, rồi chạy `./gradlew testDevDebugUnitTest assembleDevDebug` (cần JDK 21, Android SDK 34, NDK `25.1.8937393`; xem `docs/BUILD_AND_RUN.md`).
3. Thay đổi liên quan đa tab: thử với **từ 2 game chạy cùng lúc**, kể cả đóng một tab khi tab kia đang chạy.
4. Cập nhật `docs/` và `CHANGELOG.md` nếu hành vi thay đổi.
5. Mở pull request và điền mẫu có sẵn. Ghi rõ **đã thử trên máy nào, gì chưa thử**.

## Nguyên tắc

- Không ghi "đã kiểm tra" khi chưa chạy thật; một lần CI xanh không thay cho việc thử trên máy thật.
- Giữ nguyên `LICENSE` và `NOTICE`, thông báo bản quyền của J2ME Loader (Apache 2.0).
- Báo lỗi bằng mẫu Issue; vấn đề bảo mật xem `SECURITY.md`.
