# Ghi chú bàn giao

Dành cho người hoặc AI tiếp tục làm việc trên Project 4.

## Nhận diện

- Repo: `Kidtomboy/Che779-Android` (trước đây tên `Che779-Emulator`; tên đó nay là Project 1).
- Ứng dụng `com.che779.emulator`, nền J2ME Loader 1.8.2, phiên bản `1.3.0` (`versionCode 130`).
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


## Audit độc lập 1.3.0 — 2026-11-16

Darling đã rà soát source và tài liệu ở revision hiện tại. Danh sách phát hiện, phân loại mức độ xác minh và phạm vi được phép làm nằm trong [AI_CHANNEL.md](AI_CHANNEL.md) (ghi chú mới nhất ở cuối) và GitHub Issue: https://github.com/Kidtomboy/Che779-Android/issues/1.

**Quy tắc bàn giao:** audit/Issue không phải quyền tự động sửa source. Claude phải chờ Cherry duyệt từng giải pháp/phạm vi; trước khi code cần kiểm tra lại main vì source có thể thay đổi. Sau khi sửa, cập nhật DEBUG_LOG, TESTING, PROJECT_STATE và AI_CHANNEL với bằng chứng build/runtime.
