# Thử nghiệm: Emulator V1 trong Termux

**Không hỗ trợ chính thức.** Cherry thấy Termux khó thao tác nên chỉ V2 được hỗ trợ; V1 để ở đây cho ai muốn tự trải nghiệm. Chưa thử trên Termux thật.

V1 chạy **một game một cửa sổ** (không có Remoter trong Termux). Script dùng chung `termux/che779-termux.sh`, chỉ khác jar:
```bash
bash .github/scripts/fetch-emulator.sh v1        # lấy Che779-Emulator đúng commit ghim ở termux/test/EMULATOR_V1_REF, build jar
bash termux/build-termux.sh build/Che779_Emulator.jar Che779_Emulator.jar dist/che779-termux-v1-test.sh
```
Mỗi lần phát hành, workflow cũng tạo sẵn `che779-termux-v1-test.sh` trong **artifact** `termux-v1-test` của lần chạy Actions (không đưa vào Releases).
