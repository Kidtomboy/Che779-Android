# AI Audit Handoff — Project 4 (2026-11-28)

> Snapshot source/docs và test coverage cho Claude. Không tuyên bố đã xác minh runtime Android.

## Repository baseline
- Repo: Kidtomboy/Che779-Android
- VERSION: 1.3.6
- main HEAD đã đọc: f2cb2bbfd8a95a380bd77dc3a5593268cb2e671d
- Audit issue mới: https://github.com/Kidtomboy/Che779-Android/issues/4
- Issue cũ được thay đầu mối: https://github.com/Kidtomboy/Che779-Android/issues/1

## Bằng chứng docs
- docs/TESTING.md blob 2d5e248de3a2fd7ee7b94edb26bc1c1214401d76.
- docs/DEBUG_LOG.md blob 1348a27513bbeb22bf198deec89435b44792515b.
- CI chỉ chạy DiagnosticLogStore unit tests và assembleDevDebug; không phải lifecycle/multi-game suite.
- Ma trận U1–U14, L1–L10, N1–N7, T1–T6, P1–P6, S1–S5 trong TESTING phần lớn NOT_RUN.
- DEBUG_LOG vẫn ghi bàn phím mới không gõ text; Git history có commit 259d7da... thêm nhập text bằng Che779 keyboard. Cần kiểm tra implementation và thiết bị rồi cập nhật doc, không tự đánh dấu pass.
- Commits liên quan UI/keyboard/grid: 75696b4..., 259d7da..., 0e88a0d..., ada3067... (tham chiếu Git history).

## Việc còn mở
1. Multi-game composite x3/x6/x12: tái hiện mới và log mới; giả thuyết dex/classloader chưa được chứng minh.
2. Keyboard/grid/list: regression sau tab switch, grid exit, resize, rotation.
3. Session.killThreads(): class-name matching có nguy cơ ảnh hưởng game khác hoặc bỏ sót thread.
4. Final-tab UX: đối chiếu MidletThread.notifyDestroyed()/finishApp() với TESTING L6, xác định luồng mong đợi trước khi kết luận.
5. Session.current()/Authenticator isolation khi chạy nhiều game.
6. Manual update/tool-tab/proxy/shortcut tests.
7. Diagnostic logging/privacy: kiểm tra log có dữ liệu nhạy cảm và trạng thái debug instrumentation trước release.

## Ràng buộc
- Không gộp Session/lifecycle refactor vào lỗi UI.
- Thu log mới đã loại dữ liệu riêng tư.
- Phân biệt code change, build, CI, device runtime.
- Không sửa source hoặc đổi hành vi trước khi Cherry duyệt giải pháp cụ thể.
- P3 là nơi chứa Termux; không chuyển Termux về P4.
