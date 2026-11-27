# Lộ trình

## Đã có trong 1.3.0
Xem [`CHANGELOG.md`](../CHANGELOG.md).

## Việc tiếp theo (khi có yêu cầu)

- Sửa 2 lỗi đang gặp trong [`DEBUG_LOG`](DEBUG_LOG.md): bản ghép nhiều game con; bàn phím thụt.
- Thay `killThreads()` bằng nhóm luồng riêng mỗi tab; truyền ngữ cảnh mạng rõ ràng cho callback toàn cục.
- Thêm kiểm thử đa tab (trùng tên lớp, proxy có đăng nhập khác nhau, đóng một tab khi tab kia đang chạy).
- Bật mã chẩn đoán bằng cờ trong Cài đặt thay vì luôn bật.
- Lập bảng lý do cho từng quyền trong Manifest.

## Ý tưởng

Giám sát tab (báo đứng hình, mất mạng, nút mở lại), nhóm tab, hẹn giờ, xuất/nhập cấu hình, quyền "Truy cập mọi tệp" để các bản clone dùng chung thư mục. Không làm: macro.

## 1.3.0 — chia repo theo nền tảng (quyết định của Cherry 15/11/2026)
Xem `DECISIONS.md`. Đã làm trong 1.3.0: (Termux đã chuyển sang P3 ngày 2026-11-28) phát hành kèm APK clone và `SHA256SUMS.txt`.
