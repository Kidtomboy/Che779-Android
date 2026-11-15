# Ghi chú bàn giao

Dành cho người hoặc AI tiếp tục làm việc trên Project 4.

## Nhận diện

- Repo: `Kidtomboy/Che779-Android` (trước đây tên `Che779-Emulator`; tên đó nay là Project 1).
- Ứng dụng `com.che779.emulator`, nền J2ME Loader 1.8.2, phiên bản `1.2.0` (`versionCode 120`).
- Lớp vỏ Che779: `javax.microedition.shell.*`.

## Nguyên tắc

- Chủ dự án: **Cherry**. Viết tài liệu bằng tiếng Việt.
- Không ghi "đã kiểm tra" khi chưa chạy; CI xanh không thay cho thử trên máy thật.
- Giữ các quyết định ở [DECISIONS](DECISIONS.md) (không giới hạn tab, không macro, không tự mở lại tab, giữ IP redirect…).
- Mọi thay đổi phải cập nhật `CHANGELOG.md` (chỉ ghi phiên bản hiện tại) và tài liệu bị ảnh hưởng.
- 2 lỗi đang gặp **chỉ sửa khi chủ dự án yêu cầu**.

## Rủi ro

- `MicroActivity` còn rất lớn (~2.100 dòng): chỉ tách khi đã có test vòng đời.
- `Session.killThreads()` là heuristic; chỉ thay sau khi thiết kế sở hữu luồng rõ ràng và có test.
- Quyền hiển thị trên ứng dụng khác thay đổi theo hãng máy.
- Giữ nguyên `LICENSE`; kiểm tra giấy phép thư viện bên thứ ba và native trước khi phát hành rộng.
