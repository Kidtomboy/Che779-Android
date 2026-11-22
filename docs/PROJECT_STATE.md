# Trạng thái dự án — Che779 Android

**Phiên bản:** bản chính `1.3.2` (`versionCode 132`); V2 `2.0.1` (`versionCode 201`, overlay `tests/v2/overlay`). Xem bảng đối chiếu trong `AI_HANDOFF.md` (2026-11-23). Trạng thái: tạm ổn, đã phát hành thử và đang thu phản hồi.

## Tóm tắt

Ứng dụng Android chạy game J2ME đa tab, dựa trên J2ME Loader 1.8.2. Các tính năng chính (đa tab, lưới, bong bóng, treo/ẩn/ngủ, proxy theo tab) đã được chủ dự án thử trên máy thật. CI build và unit test xanh.

## Đang gặp

Xem [DEBUG_LOG](DEBUG_LOG.md): bản ghép nhiều game con, bàn phím thụt, lưới → danh sách. Chủ dự án quyết định chờ yêu cầu rồi mới sửa.

## Chưa kiểm chứng

Xem [TESTING](TESTING.md): các tính năng ghi "chưa được xác nhận" trong `CHANGELOG.md`, cách ly nhiều game cùng lúc, vòng đời, quyền trên từng hãng máy.

## Việc lớn còn lại

Thay `killThreads()` bằng cách quản lý luồng rõ ràng; tách `MicroActivity` khi đã có test vòng đời; bảng quyền Manifest; tắt mã chẩn đoán mặc định.

## Nguồn sự thật

Mã nguồn và commit là bằng chứng về cách làm; log CI là bằng chứng về build; chỉ máy thật mới là bằng chứng về hành vi. Khi mâu thuẫn, tin mã nguồn và CI.
