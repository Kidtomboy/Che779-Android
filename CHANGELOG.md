# Nhật ký thay đổi

Chỉ ghi những gì **phiên bản hiện tại** có. Lịch sử các bản trước không còn được lưu ở đây.

## [1.3.0] — Bản phát hành

Nền: J2ME Loader 1.8.2. Gói `com.che779.emulator`, `versionCode 130`.

### Mới trong 1.3.0
- **Xin quyền lần đầu mở app** (từng bước, mỗi quyền hỏi một lần, từ chối không chặn app): bong bóng/hiển thị trên ứng dụng khác → thông báo → bỏ tối ưu pin.
- Quay lại từ cửa sổ nổi (chế độ danh sách): dựng lại khung game và áp lại thanh công cụ/thanh hệ thống/vị trí lớp phím J2ME để hết lệch/ghost (giả thuyết: thanh tác vụ ẩn/hiện làm lệch bố cục).
- Sửa lỗi "SetCurrentEvent … child already has a parent" (màn hình ma, không bấm được OK) khi quay lại từ cửa sổ nổi/đa nhiệm: gỡ view khỏi cha cũ trước khi gắn và dựng lại view khi nối lại phiên đang chạy.
- **Cửa sổ nổi**: kéo ngón trên bất kỳ nút nào của thanh công cụ là di chuyển được cửa sổ (chạm nhẹ vẫn là bấm nút); nút thu nhỏ theo bề ngang cửa sổ (22–34dp).
- **Chia dex nhỏ hơn**: mỗi file dex tối đa 150 class (trước 500), tối đa 6 luồng song song; jar trên 300 class mới chia.
- **Sửa bản ghép x6**: tách dex theo từng file .jar thay vì giải nén ra thư mục (tránh mất class do trùng tên hoa/thường). Cần cài lại bản ghép.
- **Thanh nút cửa sổ nổi**: `<` `>` + ô số tab (chạm để chọn tab), `*` (bản ghép), proxy ⇄, đồng bộ ô vuông (hộp thoại chọn tab), khởi động lại tab ⟳ (có nút Đổi proxy). Hộp thoại hiện trên cửa sổ nổi.
- **Cài đặt mới**: Treo Online (chế độ pin, cấp độ ẩn), Bóng nổi (chỉ hiện khi treo, kích thước, độ trong), Tối ưu hiệu năng (Headless). Sửa lỗi bàn phím J2ME "ma" sau khi đóng bàn phím Nokia (giờ ẩn hẳn).
- **Bàn phím Nokia và Android** (hai biểu tượng riêng), chọn ở thanh dưới hoặc ngay trên bàn phím.
- **Thanh icon phía dưới** gom mọi chức năng; thanh tab phía trên chỉ còn số tab.
- **Khởi động lại tab/nhập số tab** trong menu ba chấm; tab cũ được đóng hẳn cùng các kết nối trước khi mở lại.
- **Bong bóng hiện ngay khi vào game**; nhấn giữ chỉ mở menu, chạm để thu game vào cửa sổ nổi.
- **Chạy ngầm** kể cả khi xóa khỏi recents; chỉ dừng hẳn bằng "Thoát" ở menu chính hoặc nút "Dừng" ở thông báo.
- **Thông báo** có nút "Chế độ" và "Dừng", hiện chế độ pin hiện tại.
- **Nhập game từ file ZIP** ở menu chính.
- Sửa lỗi treo giao diện khi thoát tab cuối.
- **Termux (chỉ V2):** thư mục `termux/` với script một file `che779-termux.sh` (chuyển từ Che779-Linux, bỏ phần Linux). Mã V2 không chép vào repo: Actions lấy từ Che779-EmulatorV2 đúng commit ghim ở `termux/EMULATOR_V2_REF`. V1 thử nghiệm ở `termux/test/` (không hỗ trợ chính thức, không đăng ở Releases).
- **Phát hành tự động** khi gắn thẻ `vX.Y.Z`: APK chính, APK clone (`com.che779.emulatorv2`, dữ liệu `J2ME-Che779v2`), `che779-termux.sh` và `SHA256SUMS.txt` (tính sau cùng trên đúng các file đăng).

### Tính năng của ứng dụng

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
