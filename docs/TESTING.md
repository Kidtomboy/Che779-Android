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
