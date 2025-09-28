#!/usr/bin/env bash
# Runs GrepWise locally and executes JMeter performance tests.
# Produces HTML dashboards, CSV results, and a summary. Stops the app on exit.
#
# Usage:
#   chmod +x scripts/perf/run-perf-local.sh
#   scripts/perf/run-perf-local.sh
#
# Optional environment overrides:
#   GW_HOST=localhost GW_HTTP_PORT=8080 GW_SYSLOG_PORT=1514 USERS=10 DURATION=60 RAMP_UP=10 \
#   scripts/perf/run-perf-local.sh
#
# Notes:
# - Requires Java and Maven. Frontend will be built by Maven.
# - JMeter plans and perf profile are already in pom.xml.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

GW_HOST="${GW_HOST:-localhost}"
GW_HTTP_PORT="${GW_HTTP_PORT:-8080}"
GW_SYSLOG_PORT="${GW_SYSLOG_PORT:-1514}"
USERS="${USERS:-10}"
DURATION="${DURATION:-60}"
RAMP_UP="${RAMP_UP:-10}"

JAR="target/grepwise-0.0.1-SNAPSHOT.jar"
APP_LOG="app.log"
APP_PID="app.pid"

function stop_app() {
  if [[ -f "$APP_PID" ]]; then
    echo "Stopping GrepWise (PID $(cat "$APP_PID"))..."
    kill "$(cat "$APP_PID")" 2>/dev/null || true
    rm -f "$APP_PID"
  fi
}
trap stop_app EXIT

echo "==> Building application (skip unit tests)"
mvn -B -DskipTests package

if [[ ! -f "$JAR" ]]; then
  echo "ERROR: Built jar not found at $JAR" >&2
  exit 1
fi

echo "==> Launching GrepWise ($JAR)"
nohup java -jar "$JAR" >"$APP_LOG" 2>&1 &
echo $! > "$APP_PID"

# Wait for health
echo "==> Waiting for application health at http://$GW_HOST:$GW_HTTP_PORT/actuator/health"
for i in {1..90}; do
  if curl -fsS "http://$GW_HOST:$GW_HTTP_PORT/actuator/health" | grep -q '"status":"UP"'; then
    echo "==> Application is healthy."
    break
  fi
  sleep 2
  if [[ $i -eq 90 ]]; then
    echo "ERROR: Application did not become healthy in time. See $APP_LOG" >&2
    exit 1
  fi
done

# Optionally pre-create a UDP syslog source via REST if API requires it in your setup.
# For defaults, UDP listener 1514 may already be active; adjust if needed.

echo "==> Running JMeter perf tests (users=$USERS duration=${DURATION}s rampUp=${RAMP_UP}s)"
# Safety timeout to avoid indefinite hangs: total = rampUp + duration + 30s buffer
TOTAL_TIMEOUT=$((RAMP_UP + DURATION + 30))
if command -v timeout >/dev/null 2>&1; then
  echo "==> Using timeout ${TOTAL_TIMEOUT}s to guard the perf run"
  timeout ${TOTAL_TIMEOUT}s mvn -B -Pperf-test \
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

# Build summary and compare with history if script exists (skip in CI to avoid double-append)
if [[ -z "${GITHUB_ACTIONS:-}" ]]; then
  echo "==> Generating summary and trend comparison (Java)"
  if [[ -f scripts/perf/SummarizeAndCompare.java ]]; then
    java scripts/perf/SummarizeAndCompare.java || true
  else
    echo "WARN: Java summarizer not found. Skipping summary generation." >&2
  fi
else
  echo "==> Detected GitHub Actions environment; deferring summary generation to workflow step"
fi

REPORT_DIR="target/jmeter"
echo "\n==> Done. Key outputs:"
echo "- HTML dashboards: $REPORT_DIR/reports/<plan>/index.html"
echo "- Raw CSV results: $REPORT_DIR/results/"
echo "- Perf summary:    $REPORT_DIR/perf-summary.md (and JSON if generated)"
echo "- App log:         $APP_LOG"

echo "==> Tip: open the main search report if available:"
MAIN_REPORT=$(ls -1 $REPORT_DIR/reports 2>/dev/null | head -n1 || true)
if [[ -n "$MAIN_REPORT" ]]; then
  echo "   file://$ROOT_DIR/$REPORT_DIR/reports/$MAIN_REPORT/index.html"
fi

exit 0
