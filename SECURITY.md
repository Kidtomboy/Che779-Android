# Chính sách bảo mật

## Báo lỗi bảo mật

Nếu bạn tìm thấy lỗ hổng, **đừng đăng công khai** trong Issue. Hãy dùng mục *Security → Report a vulnerability* của repo trên GitHub để báo riêng. Khi báo, ghi rõ phiên bản, máy và Android, các bước tái hiện, và xóa thông tin cá nhân hoặc mật khẩu proxy khỏi log.

## Cần biết khi dùng

| Vấn đề | Mức độ | Ghi chú |
|---|---|---|
| APK phát hành ký bằng **khóa thử nghiệm công khai** | Trung bình | Để các bản cài đè lên nhau được. Ai cũng có khóa này nên chữ ký **không** chứng minh nguồn gốc; hãy tải APK từ Releases của repo chính thức và kiểm tra bằng `SHA256SUMS.txt`. |
| Quyền ứng dụng rộng và cho phép lưu lượng không mã hóa (`usesCleartextTraffic`) | Trung bình | Cần cho game J2ME dùng kết nối thô; chưa có bảng lý do từng quyền. |
| Mật khẩu proxy lưu trên máy | Thấp–Trung bình | Chưa rà soát cách lưu. |
| Hai game trùng tên lớp có thể ảnh hưởng nhau khi đóng tab | Thấp | Xem `docs/DEBUG_LOG.md`. |

Chỉ cài game J2ME từ nguồn bạn tin cậy: game chạy trong ứng dụng này có thể kết nối mạng.

## Tải bản chính thức

Chỉ tải file từ mục **Releases** của repo chính thức.
