# Nguồn gốc mã và giấy phép

- **J2ME Loader** (Nikita Shakarun và cộng tác viên) — Apache License 2.0; nền của ứng dụng, phiên bản 1.8.2.
- **TinySoundFont** (schellingb) — `app/src/main/cpp/mmapi_tsf/TinySoundFont`.
- Các thành phần khác nằm sẵn trong cây mã J2ME Loader; giữ nguyên thông báo bản quyền trong từng file.
- Các chức năng do Che779 tự viết: chế độ Treo, hẹn giờ, bong bóng, chia nhiều màn hình, Sleep Tab, proxy theo game, dọn RAM nền.
- Phần Che779: lớp vỏ đa tab `javax/microedition/shell/*`, đổi tên ứng dụng và `applicationId`, cấu hình build tự động.

Xem `LICENSE`. Việc rà soát giấy phép đầy đủ của thư viện bên thứ ba và thư viện native trước khi phát hành rộng vẫn chưa làm.
