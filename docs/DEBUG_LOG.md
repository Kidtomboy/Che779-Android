# Lỗi và rủi ro đã biết

Chủ dự án quyết định: các lỗi dưới đây **chưa sửa, chờ có yêu cầu**.

## Lỗi đang gặp

| # | Lỗi | Chi tiết |
|---|---|---|
| 1 | **Bản ghép nhiều game con (x3/x6/x12) không vào được game con** | Menu ghép hiện nhưng chạm chỉ sáng dòng. Log: 221 lớp có trong jar gốc báo "không tìm thấy" (package `b`…), rồi luồng khởi động kết thúc im lặng. Nghi: file dex chia nhiều phần thiếu/không nạp đủ, hoặc dex cũ còn lại từ lần cài trước. Chưa có log mới để kết luận. Chỉ sửa khi có người trả tiền. |
| 2 | **Bàn phím J2ME gốc thụt xuống sau khi thoát chế độ lưới** | Đang che bằng bàn phím ảo mới và mục menu "Sửa bàn phím ảo". Chưa tìm nguyên nhân gốc (cần `che779-diag.txt` ngay sau lỗi). |
| 3 | Lưới → danh sách có thể trắng màn hình | Đã đổi cách dựng lại view; chưa xác nhận. |

## Hạn chế

- Bàn phím ảo mới **không gõ chữ** vào ô nhập (Form/TextBox dùng ô nhập Android); trên màn hình không phải Canvas chỉ gửi mũi tên + OK.
- `Session.killThreads()` đoán luồng theo tên lớp; hai game trùng tên lớp có thể ngắt nhầm; game cố tình bỏ qua ngắt vẫn có thể sống.
- `Session.current()` rơi về tab đang hiện; `Authenticator` toàn cục có thể lấy nhầm tài khoản proxy khi chạy ở luồng không thuộc tab nào.
- Quyền Manifest rộng + `usesCleartextTraffic=true` chưa có bảng lý do; `MemoryGuard` gọi `System.gc()` chưa đo hiệu quả.
- Chế độ pin Headless chỉ là mức đơn giản (ngừng vẽ + đưa ứng dụng về nền).
- Chưa có đồng bộ chạm (chỉ đồng bộ phím, mặc định tắt).
- Hai bản clone không dùng chung dữ liệu (cài trùng game sẽ tốn gấp đôi dung lượng).
- Tùy chọn đổi thư mục dữ liệu chỉ có tác dụng ở chế độ lưu trữ cũ (Android 10 trở xuống).
- Mã chẩn đoán (`Che779Diag`, log nạp lớp, đếm lớp dex, thread dump) vẫn bật; nên chuyển sang bật bằng cờ trong Cài đặt trước khi phát hành rộng.
