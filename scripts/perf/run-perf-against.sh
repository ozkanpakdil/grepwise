#!/usr/bin/env bash
# Runs JMeter performance tests against an already running GrepWise instance.
# Does not build or start the application.
#
# Usage:
#   chmod +x scripts/perf/run-perf-against.sh
#   scripts/perf/run-perf-against.sh
#
# Optional environment overrides:
#   GW_HOST=localhost GW_HTTP_PORT=8080 GW_SYSLOG_PORT=1514 USERS=10 DURATION=60 RAMP_UP=10 \
#   scripts/perf/run-perf-against.sh

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

GW_HOST="${GW_HOST:-localhost}"
GW_HTTP_PORT="${GW_HTTP_PORT:-8080}"
GW_SYSLOG_PORT="${GW_SYSLOG_PORT:-1514}"
USERS="${USERS:-10}"
DURATION="${DURATION:-60}"
RAMP_UP="${RAMP_UP:-10}"

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
# Safety timeout to avoid indefinite hangs. JMeter runs multiple plans sequentially.
# Estimate total = (rampUp + duration + 30s buffer) * PLANS + 60s report buffer
PLANS=${PLANS:-3}
TOTAL_TIMEOUT=$(((RAMP_UP + DURATION + 30) * PLANS + 60))
if command -v timeout >/dev/null 2>&1; then
  echo "==> Using timeout ${TOTAL_TIMEOUT}s to guard the perf run (plans=${PLANS})"
  timeout ${TOTAL_TIMEOUT}s $MVN -B -Pperf-test \
    -Dgw.host="$GW_HOST" \
    -Dgw.http.port="$GW_HTTP_PORT" \
    -Dgw.syslog.port="$GW_SYSLOG_PORT" \
    -Dusers="$USERS" \
    -DrampUp="$RAMP_UP" \
    -DdurationSeconds="$DURATION" verify
else
  mvn -B -Pperf-test \
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

REPORT_DIR="target/jmeter"
echo "\n==> Done. Key outputs:"
echo "- HTML dashboards: $REPORT_DIR/reports/<plan>/index.html"
echo "- Raw CSV results: $REPORT_DIR/results/"
echo "- Perf summary:    $REPORT_DIR/perf-summary.md (and JSON if generated)"

exit 0
