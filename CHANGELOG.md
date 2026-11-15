# Nhật ký thay đổi

Chỉ ghi những gì **phiên bản hiện tại** có. Lịch sử các bản trước không còn được lưu ở đây.

## [1.2.0] — Bản phát hành

Nền: J2ME Loader 1.8.2. Gói `com.che779.emulator`, `versionCode 120`.

### Đã được chủ dự án thử trên máy thật
- **Đa tab:** mỗi game là một phiên riêng trong cùng một ứng dụng; cấu hình hiển thị và mạng riêng từng tab.
- **Thanh tab dạng danh sách:** đổi tên tab bằng chạm giữ, nhân bản tab, chọn game.
- **Chế độ lưới:** xem nhiều game cùng lúc, chuyển qua lại với danh sách.
- **Bong bóng nổi:** thu game thành bong bóng kéo được; chạm để mở cửa sổ game nổi (đổi cỡ, thu nhỏ).
- **Treo, ẩn, ngủ màn hình, quản lý bộ nhớ:** Treo (ngừng vẽ), nhiều mức ẩn, ngủ màn hình đen, tự dọn bộ nhớ, cảnh báo RAM thấp.
- **Proxy theo tab và chuyển hướng IP:** SOCKS5/HTTP, danh sách proxy, gán hàng loạt.

### Có trong bản này, chưa được xác nhận trên máy thật
- Đồng bộ phím giữa các tab (tick từng tab).
- Bàn phím ảo kiểu J2ME (chế độ lưới, cửa sổ nổi, thay thế bàn phím gốc khi bị lỗi).
- Số tab cố định (đóng tab 3 rồi mở lại vẫn là tab 3); dữ liệu tài khoản theo số tab.
- Thoát hẳn có hỏi *Thoát hết / Đóng tab này*.
- Đường dẫn `c:/` → `J2ME-Che779/`, dự phòng `fs/c/` rồi `/sdcard/`.
- Bong bóng: kéo vào ✕ để tắt, chạm đúp mở ứng dụng; ô Cài đặt nhanh; 4 chế độ pin (Bình thường, Tiết kiệm, Siêu tiết kiệm, Headless).
- Bản clone: workflow tạo thêm bản cài độc lập (tên gói, tên app, thư mục dữ liệu riêng).

### Bảo mật và độ ổn định
- Workflow clone kiểm tra dữ liệu nhập; khóa ký thử chỉ dùng cho bản thử, bản release chỉ ký khi được cấp đủ 4 biến `CHE779_RELEASE_*`.
- Nhật ký chẩn đoán xoay vòng (tối đa 3 MiB mỗi thế hệ); giới hạn thread dump 3 lần/tab và 20 lần/tiến trình.
- Cấu hình mạng từ chối cổng ngoài 1–65535, địa chỉ IPv6 và giao thức lạ.

### Phát hành
- Mỗi lần gắn thẻ `vX.Y.Z`, GitHub tự kiểm thử, build và tạo `Che779-Android-<phiên bản>.apk` cùng `SHA256SUMS.txt`.
