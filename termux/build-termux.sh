#!/usr/bin/env bash
# Ghép script Termux hoàn chỉnh = bản nguồn + jar nhúng (base64).
# Dùng:  termux/build-termux.sh <jar> <tên-jar-trong-script> <file-ra>
#   V2 (chính thức):  termux/build-termux.sh Che779_EmulatorV2.jar Che779_EmulatorV2.jar dist/che779-termux.sh
#   V1 (thử nghiệm):  termux/build-termux.sh Che779_Emulator.jar   Che779_Emulator.jar   dist/che779-termux-v1-test.sh
# Mã SHA-256 được TÍNH TỪ CHÍNH FILE jar rồi điền vào script — không ai dán tay.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${1:?Thiếu jar}"; NAME="${2:?Thiếu tên jar}"; OUT="${3:?Thiếu file ra}"
[ -f "$JAR" ] || { echo "Không thấy jar: $JAR" >&2; exit 1; }
VER="$(tr -d '[:space:]' < "$ROOT/VERSION")"
SHA="$(sha256sum "$JAR" | cut -d' ' -f1)"
mkdir -p "$(dirname "$OUT")"
sed -e "s/@VERSION@/$VER/" -e "s/@JAR_SHA256@/$SHA/" -e "s/@JAR_NAME@/$NAME/g" "$ROOT/termux/che779-termux.sh" > "$OUT"
base64 -w 76 "$JAR" >> "$OUT"
chmod +x "$OUT"
bash -n "$OUT"
echo "OK: $OUT  (phiên bản $VER, jar SHA-256 $SHA)"
