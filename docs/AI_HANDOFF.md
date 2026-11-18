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

## Chỉnh sửa theo yêu cầu của Cherry — 2026-11-18 (v1.3.0, versionCode 130 giữ nguyên)

Cherry giao 10 mục (file yêu cầu "CHỈNH SỬA PROJECT 4"). Đã viết code trực tiếp trên `main`. **Chưa build và chưa thử trên máy thật**: môi trường của Claude không có Android SDK, chỉ CI biên dịch được. Việc cần làm tiếp: xem CI, cài APK và thử từng mục dưới đây.

| Mục | Cách làm | Tệp chính |
|---|---|---|
| 1. Bàn phím Nokia / Android | `KeypadFactory` dựng bàn phím theo loại (lưu `che779_keypad_type`). Nokia dùng `KeypadView` cũ, Android là `QwertyKeypadView` (chữ gửi mã ký tự, mũi tên, OK, soft key). Biểu tượng riêng: `ic_c_dialpad` (Nokia) và `ic_c_keyboard` (Android), không dùng ô vuông 9 phần. | `KeypadFactory`, `QwertyKeypadView`, `BottomBar` |
| 2. Khởi động lại tab | Mục menu ba chấm "Khởi động lại tab/nhập số tab". `restartSession`: đóng socket/HTTP của tab (`Session.closeNet`), yêu cầu game thoát, gỡ tab, dừng luồng, giải phóng, rồi mới `addSession` cùng số tab sau 1,2 giây. Quá 3,5 giây mà game không thoát thì gỡ cưỡng bức. `NetHooks` ghi nhận kết nối của từng tab. Cài đặt proxy lưu theo số tab vẫn giữ, chỉ kết nối đang chạy bị đóng. | `MicroActivity`, `Session`, `NetHooks` |
| 3. Bong bóng khi vào game | `showInAppBubble()` hiện bong bóng ngay khi mở game (cần quyền hiển thị trên ứng dụng khác). Chạm bong bóng ở trạng thái này thu game vào cửa sổ nổi. | `MicroActivity`, `FloatingController` |
| 4. Chạy ngầm | `onDestroy` không dừng service/bong bóng khi app bị xóa khỏi recents mà vẫn còn game. Mở lại từ bong bóng thì `reattachRunningSessions()` gắn lại các tab. Menu chính "Thoát" gọi `BubbleService.quitGames` để dừng hẳn tiến trình game. **Rủi ro cao**, cần thử kỹ. | `MicroActivity`, `BubbleService`, `AppsListFragment` |
| 5. Nhập ZIP | Mục "Nhập game từ file ZIP" ở menu chính, chỉ lấy `.jar/.jad/.kjx` theo tên file (chống ../), nhiều game thì cho chọn, rồi mở hộp cài đặt có sẵn. | `AppsListFragment` |
| 6. Thông báo | Hai nút "Chế độ" (đổi chế độ pin) và "Dừng" (thoát app). Nội dung luôn ghi "Chế độ pin: …" và cập nhật ngay khi đổi. | `BubbleService` |
| 7. Chạm / nhấn giữ bong bóng | Nhấn giữ chỉ mở menu (cờ `longFired`), không còn mở cửa sổ nổi. Menu có dòng hướng dẫn thao tác. | `FloatingController` |
| 8. Thoát tab cuối | Nguyên nhân (đọc từ source): `notifyDestroyed` quyết định theo `Session.count()` trên luồng game, còn việc gỡ tab chạy sau trên luồng UI, nên khi đóng tab nhanh hoặc đóng tab cuối qua đường nhiều tab thì `removeSession` không còn tab kế bên và không ai đóng activity, nên khung hình tab cũ vẫn nằm đó. Sửa: nếu hết tab thì dọn view, ẩn bàn phím và gọi `MidletThread.finishApp()` (đóng activity, kết thúc tiến trình). | `MicroActivity.removeSession`, `MidletThread` |
| 9. Thanh công cụ | `TabBar` (trên) chỉ còn số tab. `BottomBar` (dưới) chứa toàn bộ icon, mỗi icon một ô rộng bằng nhau nên không chồng nhau: bong bóng, đóng tab, thêm tab, danh sách/lưới, chọn game, hai bàn phím, đồng bộ phím và proxy (chỉ ở chế độ lưới), menu. `GridBar` đã xóa. | `TabBar`, `BottomBar`, `MicroActivity` |

### Chưa kiểm chứng / có thể còn lỗi
- Toàn bộ chưa biên dịch và chưa chạy trên máy thật.
- Bàn phím ảo J2ME (VirtualKeyboard) tính vị trí theo màn hình đầy đủ; thanh dưới 40dp có thể che hàng phím thấp nhất ở chế độ danh sách.
- Mục 4: sau khi activity bị hủy, một số lệnh của game vẫn tham chiếu activity cũ; cần thử trên máy thật.
- Mục 2: cần thử với game thật dùng socket/HTTP.

## Cập nhật 2026-11-18 (2)
- `closeKeypad()` không còn khôi phục VK J2ME (sửa ghost button).
- Settings mới: pref_power_mode, pref_hide_level (String), pref_bubble_only_hang, pref_bubble_size, pref_bubble_alpha, pref_headless. Áp dụng ở MicroActivity.applyPowerPrefs (một lần/activity) và FloatingController.baseSize/baseAlpha.
- Chưa làm: tuỳ chọn hành động 1 chạm/2 chạm/giữ lâu và nguồn icon PNG tuỳ chỉnh. Chưa biên dịch/chạy máy thật tại thời điểm ghi.

## Cập nhật 2026-11-18 (3)
- FloatingController: thêm callbacks onSyncDialog/onProxy/onRestart/onPickTab; nút `*` là chữ; proxy dùng ic_c_swap; sync dùng ic_c_checked/unchecked; restart dùng ic_c_sync.
- MicroActivity: `dctx()`/`showDlg()` hiển thị hộp thoại dạng overlay (TYPE_APPLICATION_OVERLAY) khi `floating`. Hộp thoại khởi động lại có nút "Đổi proxy".
- "Lỗi bản ghép": người dùng chưa mô tả lỗi cụ thể, CHƯA sửa. Chưa biên dịch/thử máy thật.
