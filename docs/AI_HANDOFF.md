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


## Bàn giao mới — Cherry giao Darling trực tiếp (2026-11-17)
- Người thực hiện hiện tại: **Darling**; nhánh làm việc: `darling/p4-session-lifecycle-and-approved-scope`; base main lúc nhận: `332304590cbf4569a0e703dbf92cb29aafadbec7`.
- Cherry yêu cầu mọi lệnh, quyết định, thay đổi và kết quả kiểm thử phải được ghi vào `AI_CHANNEL.md`, `AI_HANDOFF.md` và tài liệu chức năng liên quan (ít nhất `PROJECT_STATE.md`, `DEBUG_LOG.md`, `TESTING.md`, `CHANGELOG.md` khi phù hợp).
- **Giữ nguyên `1.3.0`, `versionCode 130`, tag `v1.3.0`.** Đây là nhãn đồng bộ 4 project theo quyết định Cherry; mức tính năng thực tế hiện tại của P4 tương đương 1.2.0. Không bump version/tag và không ghi tính năng mới như đã phát hành.
- Phạm vi được duyệt gồm đúng 7 nhóm trong mục mới nhất của `AI_CHANNEL.md`: hai bàn phím tách biệt (Nokia/J2ME và Android QWERTY/IME, IME nhập được text vào MIDlet); restart riêng tab; tự hiện bong bóng khi vào game; tiếp tục chạy nền sau khi vuốt khỏi Recents trong giới hạn Android; nhập game ZIP từ menu chính; notification đúng hai nút “Chế độ” và “Dừng”; short-tap/long-press bong bóng có hành vi tách biệt.
- Ưu tiên đầu tiên: lỗi đóng tab cuối/duy nhất để lại màn hình game cũ.
- Không thêm tính năng ngoài phạm vi. Không force-push, không ghi đè thay đổi ngoài ý muốn; làm trên nhánh riêng, chia commit nhỏ có thể rollback. Main không được coi là đã thay đổi cho đến khi Cherry duyệt/merge.
- Tại thời điểm ghi mục này: mới đọc source và ghi nhận điểm nghi vấn; chưa sửa source, chưa build, chưa chạy GUI. CI xanh không thay cho kiểm thử trên máy Android thật.

- Cập nhật sau khi bắt đầu sửa: [ĐÃ SỬA MÃ, CHƯA KIỂM THỬ] nhánh đã chỉnh `MicroActivity.java` và `MidletThread.java` để đóng tab cuối không đi qua đường kill process mặc định, dọn host và kết thúc Activity khi không còn Session; giữ lại đường thoát ứng dụng riêng và hand-off `startAfterDestroy`. Workflow build APK được mở rộng để chạy trên nhánh `darling/**`. Các commit source: `7de00b8`, `2c11dc0`, `2f48408`; workflow: `0c635a4`. Chưa có kết quả build hoặc GUI runtime.
