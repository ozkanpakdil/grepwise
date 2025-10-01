#!/usr/bin/env bash
# enable-syslog.sh — Create and start a GrepWise Syslog listener via REST
#
# Default: TCP 1514 RFC5424 at http://127.0.0.1:8080
#
# Usage examples:
#   scripts/enable-syslog.sh                                 # create/start TCP 1514
#   scripts/enable-syslog.sh -p UDP                          # UDP 1514
#   scripts/enable-syslog.sh -P 5514 -i syslog-tcp-5514      # custom port and id
#   scripts/enable-syslog.sh -H http://localhost:8081        # alternate base URL
#   scripts/enable-syslog.sh -n "My Syslog" -f RFC3164       # RFC3164 format
#
set -euo pipefail

BASE_URL="http://127.0.0.1:8080"
PORT=1514
PROTO="TCP"       # or UDP
FORMAT="RFC5424"  # or RFC3164
ID="syslog-tcp-1514"
NAME="Syslog TCP 1514"
START=1

usage() {
  cat <<USAGE
Create and start a Syslog source in GrepWise via REST.

Options:
  -H <baseUrl>   GrepWise base URL (default: ${BASE_URL})
  -P <port>      Syslog port (default: ${PORT})
  -p <proto>     Protocol TCP or UDP (default: ${PROTO})
  -f <format>    RFC5424 or RFC3164 (default: ${FORMAT})
  -i <id>        Source id (default: ${ID})
  -n <name>      Source name (default: ${NAME})
  -S             Do not start after create/update (default is to start)
  -h             Show help

Examples:
  scripts/enable-syslog.sh
  scripts/enable-syslog.sh -p UDP
USAGE
}

while getopts ":H:P:p:f:i:n:Sh" opt; do
  case "$opt" in
    H) BASE_URL="$OPTARG" ;;
    P) PORT="$OPTARG" ;;
    p) PROTO="${OPTARG^^}" ;;
    f) FORMAT="${OPTARG^^}" ;;
    i) ID="$OPTARG" ;;
    n) NAME="$OPTARG" ;;
    S) START=0 ;;
    h) usage; exit 0 ;;
    :) echo "Option -$OPTARG requires an argument" >&2; exit 1 ;;
    *) echo "Unknown option -$OPTARG" >&2; usage; exit 1 ;;
  esac
done

if [[ "$PROTO" != "TCP" && "$PROTO" != "UDP" ]]; then
  echo "[ERROR] Invalid -p <proto>. Use TCP or UDP" >&2
  exit 1
fi
if [[ "$FORMAT" != "RFC5424" && "$FORMAT" != "RFC3164" ]]; then
  echo "[ERROR] Invalid -f <format>. Use RFC5424 or RFC3164" >&2
  exit 1
fi

# Adjust default id/name if user changed port/proto but left defaults
if [[ "$ID" == "syslog-tcp-1514" ]]; then
  if [[ "$PROTO" == "UDP" ]]; then ID="syslog-udp-${PORT}"; NAME="Syslog UDP ${PORT}"; else ID="syslog-tcp-${PORT}"; NAME="Syslog TCP ${PORT}"; fi
fi

payload=$(cat <<JSON
{
  "id": "${ID}",
  "name": "${NAME}",
  "enabled": true,
  "sourceType": "SYSLOG",
  "syslogPort": ${PORT},
  "syslogProtocol": "${PROTO}",
  "syslogFormat": "${FORMAT}"
}
JSON
)

set +e
# Try to create
create_resp=$(curl -s -o /dev/stderr -w "%{http_code}" -X POST "${BASE_URL}/api/sources" \
  -H 'Content-Type: application/json' \
  -d "${payload}")
status=$?
http_code=$(tail -n1 <<<"${create_resp}")
set -e

if [[ $status -ne 0 ]]; then
  echo "[ERROR] Failed to contact ${BASE_URL} (curl exit ${status}). Is the backend running?" >&2
  exit 1
fi

if [[ "${http_code}" == "201" || "${http_code}" == "200" ]]; then
  echo "[INFO] Syslog source '${ID}' created." >&2
else
  echo "[WARN] Create returned HTTP ${http_code}. Will try update (PUT)." >&2
  # Try update
  set +e
  update_resp=$(curl -s -o /dev/stderr -w "%{http_code}" -X PUT "${BASE_URL}/api/sources/${ID}" \
    -H 'Content-Type: application/json' \
    -d "${payload}")
  http_code=$(tail -n1 <<<"${update_resp}")
  set -e
  if [[ "${http_code}" == "200" ]]; then
    echo "[INFO] Syslog source '${ID}' updated." >&2
  else
    echo "[ERROR] Update returned HTTP ${http_code}. Cannot create/update source." >&2
    exit 1
  fi
fi

if (( START )); then
  set +e
  start_resp=$(curl -s -o /dev/stderr -w "%{http_code}" -X POST "${BASE_URL}/api/sources/${ID}/start")
  http_code=$(tail -n1 <<<"${start_resp}")
  set -e
  if [[ "${http_code}" == "200" ]]; then
    echo "[INFO] Syslog source '${ID}' started on ${PROTO} ${PORT}." >&2
  else
    echo "[WARN] Start returned HTTP ${http_code}. It may already be running." >&2
  fi
fi

echo "[INFO] Done. You can now send logs, e.g.:" >&2
if [[ "$PROTO" == "UDP" ]]; then
  echo "  echo '<134>Oct 11 22:14:15 myhost myapp: hello via UDP' | nc -u -w1 localhost ${PORT}" >&2
else
  echo "  printf '<134>1 $(date -u +%Y-%m-%dT%H:%M:%SZ) myhost myapp 1234 - - hello via TCP\\n' | nc localhost ${PORT}" >&2
fi
