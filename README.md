# Che779 Emulator (Android)

Bản Android của Che779 Emulator, chạy game J2ME (.jar) trực tiếp trên điện thoại, không cần Termux hay Ubuntu.

Dự án đang ở giai đoạn thử nghiệm, phiên bản `0.0.0.2-android`.

## Tải APK
1. Vào tab **Actions** của repo, chọn lần chạy **Build Che779 APK** mới nhất (dấu tích xanh).
2. Kéo xuống mục **Artifacts**, tải `Che779-Emulator-APK` (file .zip), giải nén ra `Che779-Emulator.apk`.
3. Cài vào điện thoại (cho phép cài từ nguồn không rõ nếu máy hỏi).

Bản build dùng khóa ký thử nghiệm cố định, nên bản mới cài đè lên bản cũ được mà không mất dữ liệu.

## Nguồn gốc & giấy phép
Dựa trên [J2ME Loader](https://github.com/nikita36078/J2ME-Loader) của Nikita Shakarun và những người đóng góp, phát hành theo Apache License 2.0. Xem `LICENSE` và `NOTICE`.
Các file đã sửa đổi so với bản gốc mang thông báo thay đổi trong lịch sử git của repo này.

## Ghi công
Ý tưởng chế độ Treo, hẹn giờ tắt, ô Cài đặt nhanh và bong bóng điều khiển tham khảo từ **J2ME NST (NST Tiến)**.
Ý tưởng chia nhiều màn hình, Sleep Tab, proxy theo game và dọn RAM nền tham khảo từ **J2ME Loader v10 (Hùng Hero)**.
Mã nguồn được viết lại độc lập.
