# Kiểm thử

## Tự động

| Việc | Khi nào |
|---|---|
| `testDevDebugUnitTest`: kiểm thử `DiagnosticLogStore` (xoay vòng, giới hạn byte, giữ nguyên UTF-8) | Mỗi lần push |
| `assembleDevDebug`: build APK | Mỗi lần push |
| Kiểm tra dữ liệu nhập của workflow clone | Mỗi lần tạo clone |

## Đã kiểm chứng

| Hạng mục | Cách kiểm | Kết quả |
|---|---|---|
| Đa tab, thanh tab, lưới, bong bóng, treo/ẩn/ngủ, proxy theo tab, chuyển hướng IP | Chủ dự án thử trên máy thật | Chạy |
| Build APK và unit test | CI GitHub Actions | Xanh |

## Chưa kiểm chứng

- Các mục trong phần "chưa được xác nhận trên máy thật" của `CHANGELOG.md`.
- Hai game chạy cùng lúc: cách ly Display, EventQueue, RMS, FileConnection, mạng; đóng một tab khi tab kia đang chạy.
- Vòng đời khi xoay màn hình, ra nền/trở lại, tiến trình bị hệ điều hành tạo lại.
- Quyền hiển thị trên ứng dụng khác trên các hãng máy (MIUI, ColorOS, Samsung, Pixel) và Android 8–15.
- Proxy HTTP/SOCKS5 và chuyển hướng IP khi lỗi hoặc quá hạn.

Một lần CI xanh không thay cho việc thử trên máy thật.


## Kế hoạch kiểm thử cho phạm vi Cherry giao (2026-11-17)
- Đóng tab duy nhất: không còn Displayable/View game cũ; trạng thái tab/session/host nhất quán; Activity hiển thị trạng thái rỗng/menu theo thiết kế.
- Đóng tab đang chọn khi còn tab khác: tab kế tiếp hoạt động đúng, không ảnh hưởng game khác.
- Restart tab: chỉ MIDlet/session của tab đã chọn được restart; tab còn lại giữ nguyên trạng thái.
- Bàn phím Nokia và Android IME: hai nút riêng; IME nhập/xóa/chuyển dòng được trong TextBox/Form MIDlet; không làm mất phím game.
- Bubble: tự hiện khi vào game khi quyền overlay cho phép; nếu chưa có quyền phải xử lý hướng dẫn/cấp quyền, không tạo service trùng.
- Nền: vuốt task khỏi Recents không chủ động dừng service/session; nút “Dừng” và nút “Thoát” dừng theo đúng phạm vi; kiểm tra riêng giới hạn Android/OEM.
- Notification: có đúng hai action “Chế độ” và “Dừng”; action “Chế độ” cập nhật nội dung thể hiện mode hiện tại; “Dừng” dừng thực sự.
- Bubble gestures: tap ngắn mở danh sách tab/game; long-press mở menu treo/mức ẩn; long-press không đồng thời kích hoạt tap; kéo/thả vẫn hoạt động.
- ZIP import: chọn ZIP hợp lệ từ menu chính/danh sách game, nhập đúng và xử lý ZIP lỗi/thiếu JAR/JAD an toàn.
- Chưa có mục nào trong kế hoạch này được đánh dấu đã chạy cho đến khi có log/thiết bị/emulator và kết quả cụ thể.

- [ĐÃ SỬA MÃ, CHƯA KIỂM THỬ] Nhánh Darling đã tách thao tác đóng tab cuối khỏi đường thoát tiến trình. Workflow build đã cấu hình để chạy trên `darling/**`; cần chờ kết quả `testDevDebugUnitTest` và `assembleDevDebug`, sau đó vẫn phải thử thao tác đóng tab cuối trên Android thật/emulator.
