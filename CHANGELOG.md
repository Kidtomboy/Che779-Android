# Changelog

## 0.0.0.2-android — Implementation Pass 1

- Thống nhất `versionName` hiển thị với tên phiên bản Android trong README; tăng `versionCode` lên 103.
- Tách khóa thử nghiệm khỏi cấu hình ký `release`; release chỉ ký khi được cấp đủ thông tin keystore riêng qua environment.
- Validate package ID, tên ứng dụng và thư mục dữ liệu clone trong Gradle và GitHub Actions.
- Chạy unit tests trước khi build APK trên cả workflow APK chính và workflow clone.
- Thay cơ chế xóa trắng log chẩn đoán khi đầy bằng hai thế hệ `che779-diag.txt` và `che779-diag.txt.old`.
- Giới hạn thread dump ở tối đa 3 lần/tab và 20 lần/process.
- Bổ sung unit tests cho log rotation/giới hạn UTF-8 và hướng dẫn build dành cho developer.
- Sửa build number để source archive không có `.git` vẫn cấu hình Gradle được.

### Chưa xác minh

- Chưa chạy Android Gradle build trong môi trường hiện tại vì không có Android SDK/NDK cài sẵn.
- Chưa có kết quả CI GitHub Actions hoặc kiểm thử trên thiết bị Android thật.
- Chưa refactor `MicroActivity` hoặc thay heuristic tìm luồng của `Session.killThreads()`; đây vẫn là rủi ro cần test đa phiên chuyên sâu.
