#!/usr/bin/env bash
# ╔══════════════════════════════════════════════════════════════╗
# ║       Che779 Emulator — bản chạy cho Termux (Android)        ║
# ╚══════════════════════════════════════════════════════════════╝
# Một tệp duy nhất (jar nhúng ở cuối). Dành cho Termux trên Android; Linux dùng repo Che779-Linux.
#   bash che779.sh [tùy chọn] [đoạn_tên_game]
#   -r, --ram MB     RAM tối đa cho Java (mặc định 1024)
#   -g, --game TEXT  tự mở các game trong kho có tên chứa TEXT
#       --net        kiểm tra kết nối mạng (DNS, HTTP, cổng game) rồi thoát
#       --doctor     in thông tin môi trường rồi thoát
#       --reset      cài lại từ đầu (xóa dấu hiệu "đã cài")
#   -V, --version    in phiên bản rồi thoát
#   -h, --help       trợ giúp
# Kiểu cũ vẫn chạy: bash che779.sh 1024 build11
set -u
set -o pipefail
# Hai giá trị này do scripts/build-release.sh điền khi đóng gói (mã băm tính từ đúng jar được nhúng).
CHE_VERSION="@VERSION@"
CHE_JAR_SHA256="@JAR_SHA256@"
CHE_JAR_NAME="@JAR_NAME@"
export LC_ALL=C.UTF-8 2>/dev/null || true

# ───────────── giao diện ─────────────
if [ -t 1 ] && [ "${NO_COLOR:-}" = "" ]; then
  C0=$'\e[0m'; B=$'\e[1m'; D=$'\e[2m'
  R=$'\e[38;5;203m'; G=$'\e[38;5;78m'; Y=$'\e[38;5;221m'; BL=$'\e[38;5;75m'; M=$'\e[38;5;177m'; CY=$'\e[38;5;80m'
else C0=; B=; D=; R=; G=; Y=; BL=; M=; CY=; fi
COLS=$(tput cols 2>/dev/null || echo 70); [ "$COLS" -gt 76 ] && COLS=76; [ "$COLS" -lt 44 ] && COLS=44
line(){ local i o=''; for ((i=0;i<$1;i++)); do o+='─'; done; printf '%s' "$o"; }
pad(){ local t="$1" w=$2; printf '%s%*s' "$t" $((w-${#t})) ''; }
banner(){
  printf '\n%s╭%s╮%s\n' "$M" "$(line $((COLS-2)))" "$C0"
  for t in "$@"; do printf '%s│%s %s %s│%s\n' "$M" "$B" "$(pad "$t" $((COLS-4)))" "$M" "$C0"; done
  printf '%s╰%s╯%s\n' "$M" "$(line $((COLS-2)))" "$C0"
}
ok(){   printf '  %s✔%s %s\n' "$G" "$C0" "$*"; }
warn(){ printf '  %s▲%s %s\n' "$Y" "$C0" "$*"; }
err(){  printf '  %s✘%s %s\n' "$R" "$C0" "$*"; }
info(){ printf '  %s•%s %s\n' "$BL" "$C0" "$*"; }
kv(){   printf '  %s%s%s %s\n' "$D" "$(pad "$1" 14)" "$C0" "$2"; }
head1(){ printf '\n%s%s▎%s %s%s%s\n' "$CY" "$B" "$C0" "$B" "$*" "$C0"; }
LOG="${TMPDIR:-/tmp}/che779_setup.log"
RUN_TIMEOUT="${CHE_RUN_TIMEOUT:-1800}"
case "$RUN_TIMEOUT" in ''|*[!0-9]*|0) RUN_TIMEOUT=1800;; esac
# run "Tiêu đề" lệnh... : chạy nền, có spinner; timeout tổng thể, lỗi thì in đuôi log
run(){
  local title="$1"
  shift
  : >"$LOG"

  if command -v timeout >/dev/null 2>&1; then
    timeout "$RUN_TIMEOUT" bash -c '"$@"' _ "$@" >>"$LOG" 2>&1 &
  else
    warn "Thiếu tiện ích timeout; không thể áp giới hạn ${RUN_TIMEOUT}s cho bước này."
    bash -c '"$@"' _ "$@" >>"$LOG" 2>&1 &
  fi

  local pid=$! i=0 s='⠋⠙⠹⠸⠼⠴⠦⠧⠏' t0=$SECONDS

  if [ -t 1 ]; then
    while kill -0 "$pid" 2>/dev/null; do
      printf '\r  %s%s%s %s %s(%ds)%s\033[K' \
        "$CY" "${s:i++%10:1}" "$C0" "$title" "$D" \
        $((SECONDS-t0)) "$C0"
      sleep 0.12
    done
    printf '\r\033[K'
  fi

  wait "$pid"
  local rc=$?

  if [ "$rc" -eq 0 ]; then
    ok "$title ${D}($((SECONDS-t0))s)${C0}"
  else
    err "$title"
    tail -n 12 "$LOG" | sed 's/^/      /'
  fi

  return "$rc"
}

# ───────────── tham số ─────────────
XMX=1024; FILTER=""; MODE_NET=0; MODE_DOC=0; RESET=0
if [ "${1:-0}" -gt 0 ] 2>/dev/null && [ "${1:-0}" -le 64 ]; then shift; fi   # kiểu cũ "số_tab RAM"
while [ $# -gt 0 ]; do
  case "$1" in
    -r|--ram) XMX="${2:-1024}"; shift;;
    -g|--game) FILTER="${2:-}"; shift;;
    --net) MODE_NET=1;; --doctor) MODE_DOC=1;; --reset) RESET=1;;
    -V|--version) echo "Che779 Termux $CHE_VERSION"; exit 0;;
    -h|--help) sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'; exit 0;;
    ''|*[!0-9]*) [ -z "$FILTER" ] && FILTER="$1";;
    *) XMX="$1";;
  esac; shift
done
case "$XMX" in ''|*[!0-9]*) XMX=1024;; esac
if [ "$XMX" -lt 128 ] || [ "$XMX" -gt 16384 ]; then
  err "Giá trị RAM không hợp lệ: ${XMX} MB (cho phép 128–16384 MB)."
  exit 2
fi

sha256_of(){
  if command -v sha256sum >/dev/null 2>&1; then sha256sum "$1" | cut -d' ' -f1
  elif command -v shasum >/dev/null 2>&1; then shasum -a 256 "$1" | cut -d' ' -f1
  elif command -v openssl >/dev/null 2>&1; then openssl dgst -sha256 "$1" | sed 's/^.*= //'
  else echo ""; fi
}

extract_jar(){
  local dest="$1" tmp="$1/.@JAR_NAME@.tmp"
  mkdir -p "$dest" || return 1
  if [ -z "$(sed '1,/^__JAR__$/d' "$0" | head -c 16)" ]; then
    err "Tệp này là bản nguồn, không chứa jar. Hãy tải che779.sh ở mục Releases."; return 1
  fi
  if ! sed '1,/^__JAR__$/d' "$0" | base64 -d > "$tmp"; then
    rm -f "$tmp"; err "Không giải mã được JAR nhúng (base64 lỗi)."; return 1
  fi
  # 1) đầu tệp ZIP ("PK\003\004") — không cần cài thêm unzip; có unzip thì kiểm tra sâu hơn.
  if [ "$(head -c 4 "$tmp" | od -An -tx1 | tr -d ' \n')" != 504b0304 ] || { command -v unzip >/dev/null 2>&1 && ! unzip -tq "$tmp" >/dev/null 2>&1; }; then
    rm -f "$tmp"; err "JAR nhúng bị hỏng."; return 1
  fi
  # 2) mã SHA-256 phải khớp giá trị đã ghi lúc đóng gói
  local got; got="$(sha256_of "$tmp")"
  if [ -n "$got" ] && [ "$got" != "$CHE_JAR_SHA256" ]; then
    rm -f "$tmp"; err "Mã SHA-256 của JAR không khớp (mong đợi ${CHE_JAR_SHA256:0:12}…, thực tế ${got:0:12}…). Hãy tải lại che779.sh."; return 1
  fi
  mv -f "$tmp" "$dest/@JAR_NAME@" || { rm -f "$tmp"; return 1; }
}
if [ "${CHE_DRY:-0}" = "1" ]; then extract_jar "${CHE_OUT:-/tmp/che_dry}" || exit 1; echo "dry-run: jar extracted and ZIP structure checked"; fi

# ───────────── nhận diện hệ điều hành ─────────────
OS_KIND=unknown; DISTRO="?"; FAMILY=""; PM=""
UN=$(uname -s 2>/dev/null); UO=$(uname -o 2>/dev/null || true)
if [ -n "${CHE_FORCE_OS:-}" ]; then OS_KIND="$CHE_FORCE_OS"
elif [ "$UO" = "Android" ] || [ -d /data/data/com.termux ] || case "${PREFIX:-}" in *com.termux*) true;; *) false;; esac; then OS_KIND=termux
elif [ "$UN" = "Linux" ]; then OS_KIND=linux
elif [ "$UN" = "Darwin" ]; then OS_KIND=macos
fi
if [ "$OS_KIND" = linux ] && [ -r /etc/os-release ]; then
  . /etc/os-release; DISTRO="${PRETTY_NAME:-${NAME:-Linux}}"; FAMILY=" ${ID:-} ${ID_LIKE:-} "
  case "$FAMILY" in
    *" debian "*|*" ubuntu "*) PM=apt;; *" fedora "*|*" rhel "*|*" centos "*) PM=dnf;;
    *" arch "*|*" manjaro "*) PM=pacman;; *" alpine "*) PM=apk;; *" suse "*|*" opensuse "*) PM=zypper;;
  esac
  grep -qi microsoft /proc/version 2>/dev/null && DISTRO="$DISTRO (WSL)"
elif [ "$OS_KIND" = termux ]; then DISTRO="Termux $(uname -m)"; fi
ARCH=$(uname -m 2>/dev/null)
RAMMB=$(awk '/MemAvailable/{printf "%d",$2/1024}' /proc/meminfo 2>/dev/null)
sudo_cmd(){ if [ "$(id -u)" = 0 ]; then "$@"; elif command -v sudo >/dev/null; then sudo "$@"; else err "Cần quyền root/sudo để: $*"; return 1; fi; }
export -f sudo_cmd

# ───────────── kiểm tra mạng ─────────────
net_check(){
  head1 "Kiểm tra kết nối mạng"
  local HOSTS="teamobi.com wap.teamobi.com trochoididong.us"
  for h in $HOSTS; do
    if getent hosts "$h" >/dev/null 2>&1 || (command -v nslookup >/dev/null && nslookup "$h" >/dev/null 2>&1); then ok "DNS  $h"; else err "DNS  $h  — không phân giải được"; fi
  done
  local t=""; command -v curl >/dev/null && t=curl || { command -v wget >/dev/null && t=wget; }
  local list=""
  for u in http://teamobi.com/srvips/avatar2.txt http://trochoididong.us/srvips/avatar_C.txt; do
    local out=""
    if [ "$t" = curl ]; then out=$(curl -fsS -m 8 "$u" 2>/dev/null); elif [ "$t" = wget ]; then out=$(wget -qO- -T 8 "$u" 2>/dev/null); fi
    if [ -n "$out" ]; then ok "HTTP $u"; list="$list $out"; else err "HTTP $u  — không tải được"; fi
  done
  # danh sách máy chủ dạng ip:port (hoặc ip,port) → thử mở cổng TCP
  local n=0 pass=0
  for e in $(printf '%s' "$list" | tr ',;|' '   ' | grep -oE '[0-9]{1,3}(\.[0-9]{1,3}){3}:[0-9]{2,5}' | sort -u | head -8); do
    n=$((n+1)); if timeout 4 bash -c "exec 3<>/dev/tcp/${e%:*}/${e#*:}" 2>/dev/null; then ok "TCP  $e"; pass=$((pass+1)); else err "TCP  $e  — bị từ chối/chặn"; fi
  done
  [ $n -eq 0 ] && warn "Không đọc được danh sách máy chủ game (HTTP hỏng hoặc định dạng khác)."
  echo
  if [ $n -gt 0 ] && [ $pass -gt 0 ]; then ok "${B}Mạng ổn${C0}: $pass/$n cổng game mở."
  else warn "Mạng chưa thông tới máy chủ game. Thử đổi DNS/VPN/proxy, rồi chạy lại --net."; fi
}

doctor(){
  head1 "Môi trường"
  kv "Hệ điều hành" "$DISTRO"; kv "Loại" "$OS_KIND"; kv "Kiến trúc" "$ARCH"; kv "RAM trống" "${RAMMB:-?} MB"
  kv "Trình quản lý" "${PM:-—}"
  if [ -n "${DISPLAY:-}" ]; then kv "Màn hình" "$DISPLAY"; elif [ -n "${WAYLAND_DISPLAY:-}" ]; then kv "Màn hình" "wayland:$WAYLAND_DISPLAY"; else kv "Màn hình" "— (chưa có)"; fi
  if command -v java >/dev/null; then kv "Java" "$(java -version 2>&1 | grep -v Picked | head -1)"; else kv "Java" "chưa cài"; fi
}

banner "  C H E 7 7 9   E M U L A T O R   V 2" "  J2ME · Termux · bản $CHE_VERSION"
if [ "$OS_KIND" != termux ]; then
  err "Script này chỉ dành cho Termux (Android). Linux: dùng gói .deb/.tar.gz ở repo Che779-Linux."; exit 1; fi
doctor
[ "$MODE_DOC" = 1 ] && exit 0
[ "$MODE_NET" = 1 ] && { net_check; exit 0; }
[ "${CHE_DRY:-0}" = "1" ] && { ok "Chạy thử (dry-run) xong."; exit 0; }

# ═════════════ TERMUX ═════════════
run_termux(){
  local SHARE="$HOME/che779_share" MARK="$HOME/.che779_setup_done"
  [ "$RESET" = 1 ] && rm -f "$MARK"
  if [ ! -f "$MARK" ]; then
    head1 "Cài đặt lần đầu — cần mạng, ~5–10 phút"
    termux-setup-storage || true
    run "Cập nhật gói Termux" bash -c 'yes | pkg update -y' || exit 1
    run "Thêm kho x11-repo" pkg install -y x11-repo || exit 1
    run "Cài Termux:X11 + proot-distro" pkg install -y termux-x11-nightly proot-distro || exit 1
    if ! proot-distro login ubuntu -- true >/dev/null 2>&1; then run "Cài Ubuntu (proot)" proot-distro install ubuntu || exit 1; else ok "Ubuntu đã có sẵn"; fi
    run "Cài Java + openbox trong Ubuntu" proot-distro login ubuntu -- bash -c '
      export DEBIAN_FRONTEND=noninteractive
      apt-get update -y &&
      (apt-get install -y openjdk-17-jre || apt-get install -y openjdk-21-jre) &&
      apt-get install -y --no-install-recommends openbox x11-utils fonts-dejavu libxtst6 libxi6 libxrender1 libxrandr2 &&
      apt-get clean && rm -rf /var/lib/apt/lists/*' || { err "Chạy lại script để thử tiếp."; exit 1; }
    touch "$MARK"
  else ok "Đã cài đặt từ trước (dùng --reset để cài lại)"; fi

  head1 "Chuẩn bị file"
  extract_jar "$SHARE" || exit 1; ok "Đã giải nén và kiểm tra JAR"
  local GM="$SHARE/games"; rm -rf "$GM"; mkdir -p "$GM"
  for d in "$HOME/storage/downloads/Che779Games" "$HOME/storage/shared/Download/Che779Games" "$HOME/Che779Games"; do
    [ -d "$d" ] && cp "$d"/*.jar "$GM"/ 2>/dev/null
  done
  kv "Kho game" "Download/Che779Games  ($(ls "$GM"/*.jar 2>/dev/null | wc -l) file .jar)"
  local DATA="$HOME/storage/downloads/Che779Data"
  mkdir -p "$DATA/filesystem/c/AngelChip" "$DATA/filesystem/e" 2>/dev/null || { DATA="$SHARE/data"; mkdir -p "$DATA/filesystem/c/AngelChip" "$DATA/filesystem/e"; }
  kv "Dữ liệu" "$DATA"
  kv "acc.txt" "…/filesystem/c/AngelChip/acc.txt"
  local OPEN=()
  if [ -n "$FILTER" ]; then for j in "$GM"/*"$FILTER"*.jar; do [ -f "$j" ] && OPEN+=("/root/che779/games/$(basename "$j")"); done; info "Mở sẵn ${#OPEN[@]} game khớp '$FILTER'"; fi

  head1 "Bật màn hình ảo & emulator (tối đa ${XMX} MB RAM)"
  termux-wake-lock 2>/dev/null || true
  CHE779_X11_PID=""
  cleanup_termux_runtime(){
    termux-wake-unlock 2>/dev/null || true
    if [ -n "${CHE779_X11_PID:-}" ]; then kill "$CHE779_X11_PID" 2>/dev/null || true; CHE779_X11_PID=""; fi
  }
  trap cleanup_termux_runtime EXIT
  trap 'cleanup_termux_runtime; exit 130' INT
  trap 'cleanup_termux_runtime; exit 143' TERM
  pkill -f termux.x11 2>/dev/null || true
  termux-x11 :0 >/dev/null 2>&1 & CHE779_X11_PID=$!; sleep 3
  am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity >/dev/null 2>&1 \
    && ok "Đã mở Termux:X11" || warn "Hãy mở app Termux:X11 bằng tay (tải APK ở github.com/termux/termux-x11/releases)"
  sleep 2
  proot-distro login ubuntu --shared-tmp --bind "$SHARE:/root/che779" --bind "$DATA:/root/.che779" -- bash -c '
    export DISPLAY=:0 JAVA_TOOL_OPTIONS= MALLOC_ARENA_MAX=2
    if ! command -v xdpyinfo >/dev/null 2>&1 || ! xdpyinfo -display "$DISPLAY" >/dev/null 2>&1; then
      echo "Không kết nối được Termux:X11 trên DISPLAY=$DISPLAY. Hãy mở Termux:X11 rồi chạy lại." >&2
      exit 70
    fi
    (openbox >/dev/null 2>&1 &); sleep 1; cd /root/che779
    exec java -Xms32m -Xmx"$0"m -XX:+UseSerialGC -XX:TieredStopAtLevel=1 \
      -XX:MinHeapFreeRatio=10 -XX:MaxHeapFreeRatio=30 -XX:-UsePerfData \
      -XX:ReservedCodeCacheSize=48m -XX:MaxMetaspaceSize=128m -Xshare:auto \
      -Dche779.gamesdir=/root/che779/games -Djava.awt.headless=false \
      -Dsun.java2d.opengl=false -Dsun.java2d.xrender=false -Dsun.java2d.pmoffscreen=false \
      -Dswing.aatext=false -Dawt.useSystemAAFontSettings=off -Djava.net.preferIPv4Stack=true \
      -jar @JAR_NAME@ "${@:1}"' "$XMX" ${OPEN[@]+"${OPEN[@]}"}
  cleanup_termux_runtime
  trap - EXIT INT TERM
  ok "Emulator đã đóng."
}

run_termux
exit 0
__JAR__
