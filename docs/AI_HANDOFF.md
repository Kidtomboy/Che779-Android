# Ghi chú bàn giao

Dành cho người hoặc AI tiếp tục làm việc trên Project 4.

## Nhận diện

- Repo: `Kidtomboy/Che779-Android` (trước đây tên `Che779-Emulator`; tên đó nay là Project 1).
- Ứng dụng `com.che779.emulator`, nền J2ME Loader 1.8.2, phiên bản nguồn hiện tại `1.3.2` (`versionCode 132`); lịch sử 1.3.1/131 ở mục "Nâng lên 1.3.1" bên dưới.
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

## Bản ghép x6 không vào được (2026-11-18)
- Phân tích file Avatar_ChipMix_Full_build14_NV_fix-menu-x6.jar: 3036 mục, chỉ còn 1788 tên khác nhau khi không phân biệt hoa/thường (obfuscate: a.class/A.class...). MZMOD.MZMOD là launcher, đọc getAppProperty("0".."5") rồi Class.forName các main.AngelChip, b.main.AngelChip,... (lib.MIDlet riêng).
- Nguyên nhân khả dĩ: MultiDex giải nén class ra thư mục `.tmp/.classes` nằm trong bộ nhớ ngoài (không phân biệt hoa/thường) nên class bị ghi đè/mất. Đã đổi sang ghi mỗi chunk thành file .jar (zip giữ nguyên tên). CHƯA chạy thử trên máy: cần xoá bản đã cài rồi cài lại .jar.

- MultiDex: CLASSES_PER_DEX=150, SINGLE_DEX_MAX_CLASSES=300, tối đa 6 luồng (Cherry xác nhận bản ghép x6 vào được khi chia nhiều dex). Chưa đo hiệu năng/RAM trên máy yếu.

- FloatingController: DragBar (onInterceptTouchEvent, ngưỡng 10dp) cho kéo cửa sổ từ mọi nút; btnW co theo chiều ngang cửa sổ. Nút `*` giữ=phím nhấn giữ đã có ở commit 9b5670b. Chưa thử máy thật.

- Bản clone (từ 2026-11-22): gói `com.che779.emulator2`, tên "Che779 Emulator 2", dữ liệu `J2ME-Che779Clone` (trước đó `com.che779.emulatorv2` / `J2ME-Che779v2`, nay dành cho bản V2).
- Proxy mới (v1.3.1): `ProxyStore` (kho dùng chung: danh sách + proxy/tích bật theo tab, parser TXT/JSON/CSV), `ProxyUi` (form danh sách; bảng lưới/nổi), SOCKS4 trong `NetHooks`. Bộ đếm thời gian: `BubbleService` (pref_notif_interval). Chưa thử máy thật.
- Kiểm tra cập nhật (cả hai bản): xem `update/UpdateManager`, `res/values/update_channel.xml` và ghi chú cuối `docs/AI_CHANNEL.md` (Darling cần QA cài đè).
- Bản V2 (mã trong `tests/v2/overlay` của main, tag `dev-2.0.1`, workflow `release-v2.yml`): gói `com.che779.emulatorv2`, "Che779 Emulator V2", dữ liệu `J2ME-Che779v2`, versionName `2.0.1`, versionCode 201. Thêm: nút nguồn/xoá ở danh sách, `Config.startEmptyUi` (tab đầu trống, extra `KEY_EMPTY_UI`), GridPanel dạng thẻ (tiêu đề, ✕, proxy, dừng, phóng to, "+ Thêm tab"), chặn xoá tab cuối, chế độ thêm game (`che779_add_mode`). Cần Darling QA: giao diện trống không có game, nút nguồn dừng game nền.

- Lỗi từ video 18:42: IllegalStateException trong SetCurrentEvent khi quay lại từ cửa sổ nổi (view còn gắn ở floating host/grid). Sửa: gỡ cha cũ trước addView; reattachRunningSessions đặt Session.grid=false và clearDisplayableView. CHƯA thử máy thật.

- Video 19:00/19:01 của Cherry: lệch bố cục + lớp phím J2ME mini ghost sau khi quay lại từ cửa sổ nổi; video không ẩn thanh công cụ vẫn ổn. onResume: nếu vừa thoát floating và không ở lưới → SetCurrentEvent lại. Giả thuyết, CHƯA thử máy thật; nếu vẫn lỗi thì thử giữ ActionBar luôn hiện khi Canvas.

- Cập nhật: relayout sau thoát floating chuyển sang onWindowFocusChanged (+350ms) để chờ thanh hệ thống ổn định. Vẫn là giả thuyết.

- PermissionWizard (MainActivity.onResume): pref perm_wizard_step 0 overlay,1 thông báo,2 pin; manifest thêm REQUEST_IGNORE_BATTERY_OPTIMIZATIONS. Theo Cherry: KHÔNG thêm cảnh báo "đừng vuốt thoát khỏi đa nhiệm". Lỗi lệch bố cục sau cửa sổ nổi: Cherry xếp mức thấp, cho qua. Chưa thử máy thật.

## Nâng lên 1.3.1 — 2026-11-18
- Cherry yêu cầu nâng CHỈ Project 4: VERSION 1.3.1, versionCode 131, versionName 1.3.1. Cherry tự tạo tag v1.3.1. Release v1.3.0 đã bị ghi đè nhiều lần trước đó bằng workflow Phát hành (update_tag); từ nay bản mới đi theo thẻ v1.3.1.

- termux/che779-termux.sh: sửa tên tệp trong trợ giúp/thông báo (che779.sh → che779-termux.sh), --help không in khung ASCII, cảnh báo khi chưa cấp quyền bộ nhớ (dữ liệu lưu tạm). Test tests/test-termux.sh đạt với jar giả; KHÔNG thử trên Termux thật.

- Release: ghi chú lấy từ .github/release-notes.md (@TAG@/@VER@); workflow update_tag cũng sửa tên/pre-release/ghi chú và gỡ build-tail.log. Bản v1.3.1 từng bị đổi tên "Nhật ký build" + pre-release (trùng lúc Cherry tạo thẻ và workflow Build; nguyên nhân chính xác chưa rõ). Sandbox không có quyền sửa release qua API (403), phải qua workflow.

## Đối chiếu version — 2026-11-23 (yêu cầu Cherry, theo thư Darling)
Nguồn đối chiếu: source/Gradle, docs, tag, Release (APK thực tế chưa cài/chạy: `NOT_RUN`).
| Nguồn | Bản chính | V2 |
|---|---|---|
| `app/build.gradle` (HEAD `715daca19e8ec0e7bae1353d5a50b4d49ca82567`) | 1.3.2 / 132 | overlay: 2.0.1 / 201 |
| `VERSION` | 1.3.2 | — |
| Tag | `v1.3.2` → `c7d5f9c` | `dev-2.0.1` → `715daca`; `dev-2.0.0` → `b3bb6ef` (còn tồn tại) |
| Release | `v1.3.2` target `c7d5f9c`, APK build từ `715daca` (workflow cũ build mã mới nhất khi chạy tay) | `dev-2.0.1` target `715daca` |
- **Version inconsistency còn mở:** Release `v1.3.2` có tag ở `c7d5f9c` nhưng APK lấy từ `715daca` (các commit giữa hai mốc đổi code, gồm cập nhật cửa sổ nổi và kiểm tra cập nhật). Cần Cherry quyết định dời tag `v1.3.2` về `715daca` (Cherry tự thực hiện) hoặc phát hành lại.
- Claude không tự dời tag. Workflow đã được trả về hành vi "luôn build đúng commit của tag, không tạo/dời/xoá tag".
