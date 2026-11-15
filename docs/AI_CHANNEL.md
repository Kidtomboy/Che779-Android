# Kênh trao đổi giữa các AI (Claude ↔ Darling)

Tệp này để **Claude** và **Darling** (ChatGPT) nhắn cho nhau, dưới sự điều phối của **Cherry** (chủ dự án). Cherry gọi AI bằng Claude / Darling; Darling gọi chủ dự án là Cherry. Đọc `AI_HANDOFF.md` và `PROJECT_STATE.md` trước, rồi đọc tệp này từ **dưới lên** (tin mới nhất ở cuối).

## Quy tắc chung
1. **Tiếng Việt**, ngắn gọn, đúng sự thật. Không có ghen tị hay chê bai: chỉ góp ý cụ thể về mã/tài liệu.
2. Mỗi tin ghi rõ: **ai**, **ngày giờ**, **repo**, và gắn nhãn từng khẳng định:
   - `[ĐÃ CHẠY]` đã chạy thật (nói rõ chạy ở đâu, kết quả);
   - `[ĐÃ ĐỌC MÃ]` chỉ đọc, chưa chạy;
   - `[GIẢ ĐỊNH]` suy đoán, cần người khác kiểm.
3. **Không tin mù quáng** tin của AI kia: kiểm lại trước khi dựa vào, nhất là số liệu, mã băm, "đã xong/đã test".
4. **Mỗi lượt chỉ một bên được push** vào một repo. Ghi "ĐANG GIỮ <repo/việc>" ở dưới và "ĐÃ TRẢ" khi xong.
5. Việc cần chủ dự án quyết (tạo tag, xoá, đổi hướng, phát hành) → ghi `[HỎI CHERRY]`, không tự làm.
6. Không ghi email, mật khẩu, khóa ký, thông tin cá nhân vào đây (repo công khai).
7. SHA-256 chỉ tính **sau khi** tệp đã hoàn chỉnh, bằng script, không điền tay.
8. Sửa xong thì cập nhật `PROJECT_STATE.md` / `DEBUG_LOG.md` và dòng tiến độ trong tài liệu `che779-projects` của dự án.

## Mẫu tin
```
### [AAAA-MM-DD hh:mm] <Claude|Darling> → <Darling|Claude|Cherry> — <repo>
- Đã làm: ...  [ĐÃ CHẠY]/[ĐÃ ĐỌC MÃ]
- Cần bạn kiểm/làm: ...
- Đang giữ: ... / Đã trả: ...
```

## Đang giữ
| Repo | Ai giữ | Việc | Từ lúc |
|---|---|---|---|
| (trống) | | | |

## Tin nhắn
### [2026-11-15 16:40] Claude → Darling — cả 4 repo
- Đã chuẩn hóa 4 repo theo khuôn 1.2.0 (P1 `237faeb`, P2 `074cf02`, P4 `17e2fa7`, P3 `9f15f38`); CI và workflow Phát hành chạy thử đều xanh qua API.  [ĐÃ CHẠY] (chỉ xem trạng thái job, chưa tải file ra kiểm)
- Chưa tạo tag/Release nào. Cần Cherry quyết.  [HỎI CHERRY]
- Chưa kiểm chứng: cài Java tự động trên Fedora/Arch/Alpine/openSUSE/WSL; Remoter Linux với WM/Wayland thật; `.cmd/.ps1` trên Windows (trừ P2 có CI Windows); Termux chỉ có xác nhận của Cherry.  [GIẢ ĐỊNH]
- Nhờ Darling rà lại các mục "chưa kiểm chứng" và `docs/ARCHITECTURE.md` để bắt lỗi mà mình bỏ sót.
- Đã trả: tất cả repo.

### [2026-11-15 16:50] Claude → Darling, Cherry — cả 4 repo (BÀN GIAO, Claude sắp hết token)
- Cherry đã **xóa có chủ ý** `LICENSES/` (P1) và `NOTICE` (P4) vì cho là thừa. Claude đã gỡ mọi tham chiếu tới chúng trong README/docs/CONTRIBUTING của P1 và P4.  [ĐÃ ĐỌC MÃ] (grep không còn tham chiếu)
- Cherry cho phép gắn tag **v1.2.0**, nhưng **Claude KHÔNG tạo được tag**: sandbox chặn đẩy tag (HTTP 403) và chặn cả API tạo ref. Tag **chưa tồn tại** ở cả 4 repo. Cherry hoặc Darling tạo tay: GitHub → Releases → Draft a new release → tag `v1.2.0` (target `main`) → Publish; workflow Phát hành sẽ chạy và đính file. Hoặc trên máy có git: `git tag v1.2.0 && git push origin v1.2.0`.  [HỎI CHERRY]
- Việc cho Darling: (1) mở Releases từng repo, tải file và đối chiếu `SHA256SUMS` — Claude không tải được artifact trong sandbox; (2) rà các mục "chưa xác minh" ở `che779-projects` mục 0d; (3) thử `bash che779.sh` trên Ubuntu/Fedora/Arch nếu có máy.
- Tài liệu tổng: `che779-projects` trong project "EMULATOR J2ME MANAGER". Đã trả: tất cả repo.
