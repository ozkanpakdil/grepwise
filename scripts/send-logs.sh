#!/usr/bin/env bash
#
# send-logs.sh — Replay local log files to GrepWise Syslog listener (TCP/UDP)
#
# Description:
#   Reads lines from files under ./logs (relative to repo root) or from a specified
#   directory/file and sends them as RFC5424 syslog messages to a host:port over
#   TCP or UDP. Defaults align with GrepWise README (port 1514, TCP).
#
# Usage examples:
#   scripts/send-logs.sh                    # send ./logs/* to localhost:1514 via TCP
#   scripts/send-logs.sh -p UDP             # send via UDP
#   scripts/send-logs.sh -h 127.0.0.1 -P 1514 -p TCP
#   scripts/send-logs.sh -s ./logs/nginx/access.log
#   scripts/send-logs.sh -s ./logs -r 50    # 50 msgs/sec rate
#   scripts/send-logs.sh -n 2               # loop 2 times over files
#
# Notes:
# - No external tools required: uses Bash /dev/tcp and /dev/udp.
# - Message format: RFC5424 "<PRI>1 TIMESTAMP HOST APP PROCID - - MSG".
# - APP is derived from file name; PROCID is current PID.
# - If ./logs is empty or missing, sends small synthetic messages.
# - Default port: 1514 (unprivileged). If running as root/CAP_NET_BIND_SERVICE, you may choose 514.
#
set -euo pipefail

HOST="127.0.0.1"
PORT=1514
PROTO="TCP"       # or UDP
SOURCE_PATH="./logs"  # file or directory
RATE=0             # messages per second; 0 = as fast as possible
LOOPS=1            # how many times to iterate over source
FACILITY=16        # local0
SEVERITY=6         # info
PRI=$(( FACILITY * 8 + SEVERITY ))

usage() {
  cat <<USAGE
Replay logs to a Syslog listener on GrepWise.

Options:
  -h <host>       Target host (default: ${HOST})
  -P <port>       Target port (default: ${PORT})
  -p <proto>      Protocol TCP or UDP (default: ${PROTO})
  -s <path>       Source file or directory (default: ${SOURCE_PATH})
  -r <rate>       Messages per second (integer, 0 = unlimited; default: ${RATE})
  -n <loops>      Times to loop over the input (default: ${LOOPS})
  -f <facility>   Syslog facility number (default: ${FACILITY})
  -v <severity>   Syslog severity number (default: ${SEVERITY})
  -x              Dry run (print what would be sent)
  -?              Show this help

Examples:
  scripts/send-logs.sh -p UDP
  scripts/send-logs.sh -h 10.0.0.5 -P 1514 -p TCP -s ./logs/app.log -r 100
USAGE
}

DRY_RUN=0

while getopts ":h:P:p:s:r:n:f:v:x?" opt; do
  case "$opt" in
    h) HOST="$OPTARG" ;;
    P) PORT="$OPTARG" ;;
    p) PROTO="${OPTARG^^}" ;;
    s) SOURCE_PATH="$OPTARG" ;;
    r) RATE="$OPTARG" ;;
    n) LOOPS="$OPTARG" ;;
    f) FACILITY="$OPTARG" ; PRI=$(( FACILITY * 8 + SEVERITY )) ;;
    v) SEVERITY="$OPTARG" ; PRI=$(( FACILITY * 8 + SEVERITY )) ;;
    x) DRY_RUN=1 ;;
    ?) usage; exit 0 ;;
    :) echo "Option -$OPTARG requires an argument" >&2; exit 1 ;;
    *) echo "Unknown option -$OPTARG" >&2; usage; exit 1 ;;
  esac
done

if [[ "$PROTO" != "TCP" && "$PROTO" != "UDP" ]]; then
  echo "Invalid -p <proto>. Use TCP or UDP" >&2
  exit 1
fi

# Resolve absolute list of files to read
collect_files() {
  local path="$1"
  if [[ -f "$path" ]]; then
    echo "$path"
  elif [[ -d "$path" ]]; then
    # common log file extensions first, fallback to any file
    shopt -s nullglob
    local matches=()
    matches+=("$path"/*.log "$path"/*.out "$path"/*.txt "$path"/*)
    for f in "${matches[@]}"; do
      [[ -f "$f" ]] && echo "$f"
    done
  fi
}

FILES=( $(collect_files "$SOURCE_PATH") ) || true

if [[ ${#FILES[@]} -eq 0 ]]; then
  echo "[INFO] No log files found in '$SOURCE_PATH'. Will send synthetic sample messages." >&2
fi

HOSTNAME=$(hostname -s 2>/dev/null || echo "localhost")
PROCID=$$

now_rfc3339() {
  # RFC3339 with UTC 'Z'
  date -u +"%Y-%m-%dT%H:%M:%SZ"
}

format_msg() {
  local app="$1"; shift
  local msg="$*"
  printf "<%d>1 %s %s %s %s - - %s\n" "$PRI" "$(now_rfc3339)" "$HOSTNAME" "$app" "$PROCID" "$msg"
}

send_tcp() {
  local data="$1"
  # Syslog over TCP commonly uses octet-counted framing or newline. GrepWise example uses newline.
  # We send newline-terminated frames.
  if (( DRY_RUN )); then
    printf "%s" "$data"
    return 0
  fi
  exec 3>/dev/tcp/$HOST/$PORT || { echo "[ERROR] Cannot open TCP $HOST:$PORT" >&2; return 1; }
  printf "%s" "$data" >&3 || true
  exec 3>&-
}

send_udp() {
  local data="$1"
  if (( DRY_RUN )); then
    printf "%s" "$data"
    return 0
  fi
  exec 3>/dev/udp/$HOST/$PORT || { echo "[ERROR] Cannot open UDP $HOST:$PORT" >&2; return 1; }
  printf "%s" "$data" >&3 || true
  exec 3>&-
}

check_target() {
  # For TCP, verify the listener is accepting connections before sending.
  if [[ "$PROTO" == "TCP" ]]; then
    if (( DRY_RUN )); then
      return 0
    fi
    # Try to open and immediately close a TCP connection.
    if exec 9>/dev/tcp/$HOST/$PORT 2>/dev/null; then
      exec 9>&-
      return 0
    else
      echo "[ERROR] Cannot connect to TCP $HOST:$PORT (connection refused or unreachable)." >&2
      echo "[HINT] GrepWise's Syslog listener is not enabled by default. You need to create a Syslog source via REST (see README: 'Network Log Ingestion (Syslog TCP/UDP)')." >&2
      echo "[HINT] Example payload to create a TCP 1514 RFC5424 source:" >&2
      echo '        {"id":"syslog-tcp-1514","name":"Syslog TCP 1514","enabled":true,"sourceType":"SYSLOG","syslogPort":1514,"syslogProtocol":"TCP","syslogFormat":"RFC5424"}' >&2
      echo "[HINT] If you created a UDP listener instead, run the script with '-p UDP'." >&2
      echo "[HINT] You can also quick-test with: printf \"<134>1 $(date -u +%Y-%m-%dT%H:%M:%SZ) myhost myapp 1234 - - hello\\n\" | nc localhost $PORT" >&2
      exit 1
    fi
  fi
}

sleep_for_rate() {
  if [[ "$RATE" -gt 0 ]]; then
    # sleep for 1/RATE seconds
    awk -v r="$RATE" 'BEGIN { printf "%.6f\n", 1.0/r }' | {
      read s; sleep "$s";
    }
  fi
}

send_line() {
  local app="$1"; shift
  local line="$*"
  local payload
  payload=$(format_msg "$app" "$line")
  if [[ "$PROTO" == "TCP" ]]; then
    send_tcp "$payload"
  else
    send_udp "$payload"
  fi
}

replay_files() {
  local loop
  for (( loop=1; loop<=LOOPS; loop++ )); do
    if [[ ${#FILES[@]} -eq 0 ]]; then
      # synthetic messages
      for i in {1..20}; do
        send_line "synthetic" "hello $i from send-logs.sh"
        sleep_for_rate
      done
      continue
    fi
    for f in "${FILES[@]}"; do
      local app
      app=$(basename "$f" | sed 's/\.[^.]*$//' | tr ' ' '_' )
      while IFS= read -r line || [[ -n "$line" ]]; do
        # skip empty lines to reduce noise
        [[ -z "$line" ]] && continue
        send_line "$app" "$line"
        sleep_for_rate
      done < "$f"
    done
  done
}

echo "[INFO] Target: ${PROTO} ${HOST}:${PORT} | Source: ${SOURCE_PATH} | Files: ${#FILES[@]} | Rate: ${RATE}/s | Loops: ${LOOPS}"
check_target
replay_files

echo "[INFO] Done."
