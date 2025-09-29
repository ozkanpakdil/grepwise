#!/usr/bin/env bash
# Runs JMeter performance tests against an already running GrepWise instance.
# Does not build or start the application.
#
# Usage:
#   chmod +x scripts/perf/against.sh
#   scripts/perf/against.sh
#
# Optional environment overrides:
#   GW_HOST=localhost GW_HTTP_PORT=8080 GW_SYSLOG_PORT=1514 USERS=2 DURATION=10 RAMP_UP=3 \
#   scripts/perf/against.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

GW_HOST="${GW_HOST:-localhost}"
GW_HTTP_PORT="${GW_HTTP_PORT:-8080}"
GW_SYSLOG_PORT="${GW_SYSLOG_PORT:-1514}"
# Safer, short defaults for local sanity runs
USERS="${USERS:-2}"
DURATION="${DURATION:-10}"
RAMP_UP="${RAMP_UP:-3}"

# Decide JMeter output base directory early; use /tmp for local runs to avoid IDE file sync storms
if [[ -n "${GITHUB_ACTIONS:-}" ]]; then
  JMETER_BASE_DIR="${JMETER_BASE_DIR:-$ROOT_DIR/target/jmeter}"
else
  JMETER_BASE_DIR="${JMETER_BASE_DIR:-/tmp/grepwise-perf-$(date +%s)}"
fi

# Determine Maven command (prefer wrapper if present)
if [[ -x "./mvnw" ]]; then
  MVN="./mvnw"
elif command -v mvn >/dev/null 2>&1; then
  MVN="mvn"
else
  echo "ERROR: Maven not found. Please install Maven or include the Maven Wrapper (./mvnw)." >&2
  exit 127
fi

# Optional quick health check
if command -v curl >/dev/null 2>&1; then
  echo "==> Checking health at http://$GW_HOST:$GW_HTTP_PORT/actuator/health"
  if ! curl -fsS "http://$GW_HOST:$GW_HTTP_PORT/actuator/health" | grep -q '"status":"UP"'; then
    echo "WARN: Health check did not return UP. Continuing anyway…" >&2
  fi
fi

echo "==> Running JMeter perf tests (users=$USERS duration=${DURATION}s rampUp=${RAMP_UP}s) against $GW_HOST:$GW_HTTP_PORT"
# Optional safety timeout to avoid indefinite hangs, disabled by default to prevent abrupt kills in IDE terminals.
# Enable by setting PERF_TIMEOUT seconds (e.g., PERF_TIMEOUT=120).
if [[ -n "${PERF_TIMEOUT:-}" ]] && command -v timeout >/dev/null 2>&1; then
  echo "==> Using timeout ${PERF_TIMEOUT}s to guard the perf run"
  timeout "${PERF_TIMEOUT}s" $MVN -B -Pperf-test \
    -Djmeter.base.dir="$JMETER_BASE_DIR" \
    -Dgw.host="$GW_HOST" \
    -Dgw.http.port="$GW_HTTP_PORT" \
    -Dgw.syslog.port="$GW_SYSLOG_PORT" \
    -Dusers="$USERS" \
    -DrampUp="$RAMP_UP" \
    -DdurationSeconds="$DURATION" verify
else
  $MVN -B -Pperf-test \
    -Djmeter.base.dir="$JMETER_BASE_DIR" \
    -Dgw.host="$GW_HOST" \
    -Dgw.http.port="$GW_HTTP_PORT" \
    -Dgw.syslog.port="$GW_SYSLOG_PORT" \
    -Dusers="$USERS" \
    -DrampUp="$RAMP_UP" \
    -DdurationSeconds="$DURATION" verify
fi

echo "==> Generating summary and trend comparison (Java)"
if [[ -f scripts/perf/SummarizeAndCompare.java ]]; then
  java scripts/perf/SummarizeAndCompare.java || true
else
  echo "WARN: Java summarizer not found. Skipping summary generation." >&2
fi

REPORT_DIR="$JMETER_BASE_DIR"
echo "\n==> Done. Key outputs:"
echo "- HTML dashboards: $REPORT_DIR/reports/<plan>/index.html"
echo "- Raw CSV results: $REPORT_DIR/results/"
echo "- Perf summary:    $REPORT_DIR/perf-summary.md (and JSON if generated)"

exit 0
