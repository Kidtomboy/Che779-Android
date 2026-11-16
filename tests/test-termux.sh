#!/usr/bin/env bash
# Kiểm thử script Termux: cú pháp, trợ giúp, phiên bản, giải nén jar có kiểm tra SHA-256, và từ chối jar sai mã.
# Dùng: tests/test-termux.sh <đường-dẫn-jar>
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JAR="${1:?Thiếu đường dẫn jar}"
T="$(mktemp -d)"; trap 'rm -rf "$T"' EXIT
export CHE_FORCE_OS=termux   # CI chạy trên Linux: giả lập Termux để thử đường giải nén jar
fail(){ echo "THẤT BẠI: $*" >&2; exit 1; }

bash -n "$ROOT/termux/che779-termux.sh" || fail "bản nguồn sai cú pháp"
"$ROOT/termux/build-termux.sh" "$JAR" "$(basename "$JAR")" "$T/out/che779-termux.sh" >/dev/null
S="$T/out/che779-termux.sh"
JN="$(basename "$JAR")"

VER="$(tr -d '[:space:]' < "$ROOT/VERSION")"
[ "$(bash "$S" --version)" = "Che779 Termux $VER" ] || fail "--version sai"
bash "$S" --help | grep -q -- '--doctor' || fail "--help thiếu nội dung"

# Chạy thử: giải nén jar, kiểm tra ZIP + SHA-256, rồi thoát
CHE_DRY=1 CHE_OUT="$T/x" bash "$S" >/dev/null 2>&1 || fail "dry-run thất bại"
[ "$(sha256sum "$T/x/$JN" | cut -d' ' -f1)" = "$(sha256sum "$JAR" | cut -d' ' -f1)" ] || fail "jar giải nén khác jar gốc"

# Phải TỪ CHỐI khi mã SHA-256 ghi trong script sai
sed 's/^CHE_JAR_SHA256="[0-9a-f]*"/CHE_JAR_SHA256="0000000000000000000000000000000000000000000000000000000000000000"/' "$S" > "$T/bad.sh"
if CHE_DRY=1 CHE_OUT="$T/y" bash "$T/bad.sh" >/dev/null 2>&1; then fail "script vẫn chạy dù SHA-256 sai"; fi

# Phải TỪ CHỐI bản nguồn (không có jar)
if CHE_DRY=1 CHE_OUT="$T/z" bash "$ROOT/termux/che779-termux.sh" >/dev/null 2>&1; then fail "bản nguồn không được phép chạy"; fi

# Không được cần unzip
mkdir -p "$T/bin"; for t in bash sed base64 head od tr mkdir mv rm cut sha256sum dirname tput cat date uname id; do p="$(command -v $t || true)"; [ -n "$p" ] && ln -sf "$p" "$T/bin/$t"; done
PATH="$T/bin" CHE_DRY=1 CHE_OUT="$T/w" "$T/bin/bash" "$S" >/dev/null 2>&1 || fail "không chạy được khi thiếu unzip"
echo "ĐẠT: tất cả kiểm thử script"
