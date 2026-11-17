# Trạng thái dự án — Che779 Android

**Phiên bản:** `1.3.0` (`versionCode 130`). Trạng thái: tạm ổn, đã phát hành thử và đang thu phản hồi.

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


## Bàn giao và phạm vi đang triển khai (2026-11-17)
- Cherry giao Darling trực tiếp làm 7 nhóm yêu cầu được liệt kê trong `AI_CHANNEL.md`; ưu tiên lỗi đóng tab cuối/duy nhất để lại màn hình game cũ.
- Làm trên nhánh `darling/p4-session-lifecycle-and-approved-scope`, base `332304590cbf4569a0e703dbf92cb29aafadbec7`. Main chưa bị sửa.
- Giữ `1.3.0` / `versionCode 130` / tag `v1.3.0` để đồng bộ 4 project. Cherry xác nhận tính năng thực tế hiện tại tương đương 1.2.0; không nâng version hoặc mô tả tính năng mới như đã phát hành.
- Tình trạng khi nhận việc: [ĐÃ ĐỌC MÃ] có nghi vấn cleanup thiếu ở nhánh removeSession khi không còn tab; [ĐÃ ĐỌC MÃ] long-press bubble có thể chạy thêm nhánh tap ở ACTION_UP; [ĐÃ ĐỌC MÃ] notification hiện chỉ có nội dung, chưa có action. Chưa sửa, chưa build, chưa chạy GUI.
