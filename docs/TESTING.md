# Kiểm thử

## Tự động

| Việc | Khi nào |
|---|---|
| `testDevDebugUnitTest`: kiểm thử `DiagnosticLogStore` (xoay vòng, giới hạn byte, giữ nguyên UTF-8) | Mỗi lần push |
| `assembleDevDebug`: build APK | Mỗi lần push |
| Kiểm tra dữ liệu nhập của workflow clone | Mỗi lần tạo clone |

## Đã kiểm chứng

| Hạng mục | Cách kiểm | Kết quả |
|---|---|---|
| Đa tab, thanh tab, lưới, bong bóng, treo/ẩn/ngủ, proxy theo tab, chuyển hướng IP | Chủ dự án thử trên máy thật | Chạy |
| Build APK và unit test | CI GitHub Actions | Xanh |

## Chưa kiểm chứng

- Các mục trong phần "chưa được xác nhận trên máy thật" của `CHANGELOG.md`.
- Hai game chạy cùng lúc: cách ly Display, EventQueue, RMS, FileConnection, mạng; đóng một tab khi tab kia đang chạy.
- Vòng đời khi xoay màn hình, ra nền/trở lại, tiến trình bị hệ điều hành tạo lại.
- Quyền hiển thị trên ứng dụng khác trên các hãng máy (MIUI, ColorOS, Samsung, Pixel) và Android 8–15.
- Proxy HTTP/SOCKS5 và chuyển hướng IP khi lỗi hoặc quá hạn.

Một lần CI xanh không thay cho việc thử trên máy thật.

## Ma trận QA — Kiểm tra cập nhật (soạn 2026-11-23, chưa chạy)
Trạng thái của mọi dòng dưới đây: `NOT_RUN` (cần thiết bị Android thật; Claude không có). Người thực hiện đề xuất: Cherry/Darling. Mã nguồn: `update/UpdateManager.java`.
| ID | Kịch bản | Thiết bị/API | Cách làm | Kết quả mong đợi | Loại | Trạng thái |
|---|---|---|---|---|---|---|
| U1 | Cập nhật từ bản cũ lên mới (bản chính) | Android 8+ | Cài bản thấp hơn v1.3.6, Cài đặt → Cập nhật → Kiểm tra | Hộp thoại bản mới, tải, cài đè, dữ liệu còn | thủ công | NOT_RUN |
| U2 | Như U1 cho V2 | Android 8+ | Cài `dev-2.0.0`, kiểm tra | Thấy `dev-2.0.1`; bản chính không thấy `dev-*` | thủ công | NOT_RUN |
| U3 | Chưa cấp quyền cài từ nguồn ngoài | API 26+ | Tải xong khi chưa cấp quyền | Mở màn hình cấp quyền, không crash; kiểm tra lại sau khi cấp thì cài được | thủ công | NOT_RUN |
| U4 | Từ chối quyền | API 26+ | Quay lại không cấp | Không cài, không treo | thủ công | NOT_RUN |
| U5 | Mất mạng khi hỏi API / khi tải | bất kỳ | Bật chế độ máy bay giữa chừng | Toast lỗi, không crash, không để tệp dở | thủ công | NOT_RUN |
| U6 | Tệp rỗng/sai SHA-256 | bất kỳ | Release thử có APK sai mã băm | Báo "không khớp mã kiểm tra", tệp bị xoá, không mở trình cài | thủ công (cần Release thử) | NOT_RUN |
| U7 | Thiếu `SHA256SUMS.txt` | bất kỳ | Release thử không có tệp | Báo thiếu, không cài | thủ công | NOT_RUN |
| U8 | Release chưa có APK | bất kỳ | Release thử không APK | Bị bỏ qua, không báo "có bản mới" | thủ công | NOT_RUN |
| U9 | Kênh/gói không khớp | bất kỳ | Chạy bản clone `com.che779.emulator2` | Chỉ mở trang Releases, không tự cài | thủ công | NOT_RUN |
| U10 | Đã là bản mới nhất | bất kỳ | Kiểm tra khi trùng phiên bản | Toast "đang dùng bản mới nhất" | thủ công | NOT_RUN |
| U11 | So sánh phiên bản `1.3.6-dev-N` | bất kỳ | Bản dev có hậu tố | Hậu tố bị bỏ khi so sánh | unit (chưa có test) | NOT_RUN |
| U12 | Giới hạn API 60/giờ | bất kỳ | Kiểm tra liên tục | Toast lỗi HTTP, không crash | thủ công | NOT_RUN |
| U13 | Tự kiểm tra khi mở app | bất kỳ | Bật công tắc, mở app 2 lần trong ngày | Chỉ kiểm tra 1 lần; im lặng khi không có gì mới | thủ công | NOT_RUN |
| U14 | Log không lộ dữ liệu nhạy cảm | bất kỳ | Xem logcat | Không có token/khoá (app không dùng token) | static + thủ công | STATIC_REVIEW (chưa thủ công) |
Rủi ro còn lại: khoá ký thử nghiệm công khai (Cherry đã quyết không đổi khoá); chưa có unit test cho `compare/parse`.

## Danh sách kiểm tra lifecycle UI (soạn 2026-11-23, chưa chạy)
Mã: `GridPanel.onSizeChanged/rebuild`, `MicroActivity` (grid, cửa sổ nổi), `FloatingController`. Trạng thái: `STATIC_REVIEW` cho khâu đọc mã, còn lại `NOT_RUN`.
| ID | Kịch bản | Mong đợi |
|---|---|---|
| L1 | Kéo đổi cỡ cửa sổ nổi liên tục | Lưới xếp lại mượt, ≤2 ô/dòng, 3 ô khi ≥85% chiều ngang màn hình, không crash |
| L2 | Xoay màn hình (lưới và cửa sổ nổi) | Lưới xếp lại đúng cỡ mới, không mất tab |
| L3 | Đưa app xuống nền rồi lên lại | Lưới còn nguyên, game không bị kẹt khung đen |
| L4 | Đóng/mở cửa sổ nổi nhiều lần | Không rò view cũ, không IllegalStateException |
| L5 | Đổi tab khi đang resize | Không crash, tab đang chọn giữ nguyên |
| L6 | Đóng tab cuối (V2) | Lần 1 thoát game về ô "Mở game"; lần 2 báo "Không được xoá tab cuối cùng" |
| L7 | Danh sách ↔ màn hình game | Chuyển qua lại không mất trạng thái |
| L8 | Không có game nào chạy | Hiện ô "Mở game"; nút "+" thêm ô trống |
| L9 | Tham chiếu view cũ (`GridPanel.lastSessions`, `post` trễ) | Callback chạy sau `destroy` không làm crash (rủi ro đã nêu trong audit, chưa xác nhận) |
| L10 | Chồng hộp thoại khi bấm dồn thanh công cụ | Chỉ một hộp thoại tại một thời điểm (`DialogGate`) |
Ghi chú: `GridPanel.post()` gọi `rebuild` có thể chạy sau khi view bị gỡ; hiện có kiểm tra `getWidth()>0` nhưng chưa có kiểm tra `isAttachedToWindow()` — rủi ro `MEDIUM CONFIDENCE`, chưa tái hiện được.

## Kiểm thử bản 1.3.6 (tất cả `NOT_RUN`)
| ID | Việc | Kết quả mong đợi |
|---|---|---|
| N1 | Mở menu | 6 nhóm đúng thứ tự, có "Khóa hướng màn hình", không còn "Sửa bàn phím ảo" |
| N2 | Đồng bộ phím: đặt 2 màu cho 4 tab | Chỉ tab cùng màu nhận phím; dải màu hiện dưới tab |
| N3 | Vào game ở chế độ danh sách | Không có bàn phím J2ME gốc; bàn phím Che779 mở được |
| N4 | Đổi độ trong suốt/cỡ nút/giao diện/ảnh nền trong Cài đặt | Bàn phím áp dụng khi mở lại |
| N5 | Gõ chữ từ bàn phím Che779 vào TextBox | Ký tự, xóa lùi, xuống dòng hoạt động |
| N6 | Macro: lần đầu hiện cảnh báo; ghi 10 lần bấm → báo "10 lần bấm"; phát 1 lần, ×10, ∞, dừng, xóa; kéo cửa sổ macro | Cảnh báo chỉ một lần; số lần bấm đúng; phát liên tục không tự dừng |
| N7 | Bản V2 (chưa build): các mục trên | Hành vi giống bản chính |

## Kiểm thử phần "công cụ tab" (sau 1.3.6, tất cả `NOT_RUN`)
| ID | Việc | Kết quả mong đợi |
|---|---|---|
| T1 | Khởi động lại tab: nhập `1-3`, rồi `1,3`, rồi `abc` | 1-3 mở lại lần lượt tab 1, 2, 3; `abc` báo nhập sai; số không có thì báo không có tab |
| T2 | Khóa tab rồi thử đóng tab, khởi động lại tab đó | Bị chặn, có thông báo; tab khóa hiện biểu tượng khóa; đợt hàng loạt bỏ qua tab khóa |
| T3 | Bật "Tự khởi động lại", cho game tự thoát | Tab mở lại; sau 5 lần liên tiếp thì dừng và báo |
| T4 | Bật "Tự khởi động lại", cho một sự kiện treo > 45 giây | Tab bị khởi động lại, không lặp quá 5 lần |
| T5 | Người dùng chủ động đóng/thoát khi đang bật tự khởi động lại | Không bị mở lại |
| T6 | Trang hướng dẫn: lần mở app đầu có hỏi; Thiết lập và menu game mở được | Hiện nội dung, cuộn được; chỉ hỏi một lần |

## Kiểm thử quản lý proxy (1.3.6, tất cả `NOT_RUN`)
| ID | Việc | Kết quả mong đợi |
|---|---|---|
| P1 | Thiết lập → Quản lý proxy → Nhập file có > 1000 dòng | Thêm hết, báo số thêm/trùng/sai; không đơ |
| P2 | Lọc HTTP / SOCKS4 / SOCKS5 | Danh sách và số đếm đúng theo loại |
| P3 | Kiểm tra với kiểu Tự động, rồi từng kiểu riêng | Sống hiện ms; chết hiện ✗; Tự động sửa loại cho đúng; Dừng được |
| P4 | Xóa đã chọn / proxy chết / tất cả đang lọc | Có hỏi xác nhận, xóa đúng, danh sách lưu lại khi mở lại |
| P5 | Vào game: danh sách proxy theo tab có thấy proxy vừa nhập | Có (kể cả khi đã mở game trước đó) |
| P6 | Nút + với nhiều game | Hiện danh sách nhanh, lần bấm sau không chậm hơn |

## Kiểm thử đợt sau 1.3.6 (tất cả `NOT_RUN`)
| ID | Việc | Kết quả mong đợi |
|---|---|---|
| S1 | Bàn phím → ★ Phím tắt → + thêm nút "Chào" = 1 2 3; nhấn giữ để sửa/xóa | Bấm nút gửi 1,2,3 lần lượt; sửa/xóa được; còn sau khi mở lại |
| S2 | Thiết lập → Kiểm tra cập nhật | Hộp thoại có đường dẫn; Mở trình duyệt và Sao chép hoạt động |
| S3 | Đổi cài đặt, thêm proxy, macro, phím tắt, ảnh nền → gỡ app, cài lại (cho phép bộ nhớ) | Thư mục Che779Data có backup.json; mở lại app thì khôi phục (có thể cần mở app lần 2 nếu cấp quyền bộ nhớ sau) |
| S4 | Hướng dẫn: vuốt trái/phải, Tiếp, Quay lại, Bỏ qua | Đúng 10 trang, dấu chấm đúng trang |
| S5 | Menu game: tab chưa khóa / đã khóa | "Khóa tab" / "Mở khóa tab"; mục khởi động lại chỉ ghi "Khởi động lại tab" |
