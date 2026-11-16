#!/usr/bin/env bash
# Lấy mã Emulator đúng commit ghim và build jar. Dùng: .github/scripts/fetch-emulator.sh v2|v1
# v2 -> build/Che779_EmulatorV2.jar (Che779-EmulatorV2) · v1 -> build/Che779_Emulator.jar (Che779-Emulator)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
case "${1:?v2|v1}" in
  v2) REPO=Che779-EmulatorV2; REF="$ROOT/termux/EMULATOR_V2_REF"; SCRIPT=.github/scripts/build-emulator-v2.sh; SRC=apps/emulator-v2/source/Che779_EmulatorV2.jar;;
  v1) REPO=Che779-Emulator;   REF="$ROOT/termux/test/EMULATOR_V1_REF"; SCRIPT=.github/scripts/build-emulator.sh; SRC=apps/emulator/source/Che779_Emulator.jar;;
  *) echo "v2|v1" >&2; exit 2;;
esac
D="$ROOT/build/src-$1"; rm -rf "$D"; mkdir -p "$ROOT/build"
git clone --quiet "https://github.com/Kidtomboy/$REPO.git" "$D"; git -C "$D" checkout --quiet "$(tr -d '[:space:]' < "$REF")"
bash "$D/$SCRIPT" | grep -v '^Picked up' || true
cp "$D/$SRC" "$ROOT/build/"; echo "OK: build/$(basename "$SRC")"
