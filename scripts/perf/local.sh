#!/usr/bin/env bash
# Starts GrepWise locally and executes JMeter performance tests.
# Produces HTML dashboards, CSV results, and a summary. Stops the app on exit.
#
# Usage:
#   chmod +x scripts/perf/local.sh
#   scripts/perf/local.sh
#
# Optional environment overrides:
#   GW_HOST=localhost GW_HTTP_PORT=8080 GW_SYSLOG_PORT=1514 USERS=2 DURATION=10 RAMP_UP=3 \
#   scripts/perf/local.sh
#   - RAMP_UP: ramp-up time in seconds to start all USERS. Example: USERS=10 RAMP_UP=10 -> ~1 user starts per second.
#
# Notes:
# - Requires Java and Maven. This script does NOT build the npm/frontend; it only starts the Spring Boot backend.
# - JMeter plans and perf profile are already in pom.xml.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

GW_HOST="${GW_HOST:-localhost}"
GW_HTTP_PORT="${GW_HTTP_PORT:-8080}"
GW_SYSLOG_PORT="${GW_SYSLOG_PORT:-1514}"
# Safer, short defaults for local sanity runs to avoid IDE freezes
USERS="${USERS:-2}"
DURATION="${DURATION:-2}"
RAMP_UP="${RAMP_UP:-1}"

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

# Start backend without packaging to avoid building the npm frontend
# We intentionally use spring-boot:run so that the prepare-package phase (where frontend builds) is not invoked.
echo "==> Starting GrepWise backend (skip unit tests; no frontend/npm build)"
# Do NOT enable Spring Boot debug or TRACE logging during perf runs.
nohup $MVN -B -ntp -DskipFrontend=true -DskipTests -Dspring-boot.run.jvmArguments="-Drate.limiting.enabled=false" spring-boot:run >"$APP_LOG" 2>&1 &
echo $! > "$APP_PID"

# Wait for health
echo "==> Waiting for application health at http://$GW_HOST:$GW_HTTP_PORT/actuator/health"
for i in {1..90}; do
  if curl -fsS "http://$GW_HOST:$GW_HTTP_PORT/actuator/health" | grep -q '"status":"UP"'; then
    echo "==> Application is healthy."
    break
  fi
  sleep 5
  if [[ $i -eq 90 ]]; then
    echo "ERROR: Application did not become healthy in time. See $APP_LOG" >&2
    exit 1
  fi
done

echo "==> Running JMeter perf tests (users=$USERS duration=${DURATION}s rampUp=${RAMP_UP}s)"
PERF_TIMEOUT=120
echo "==> Using timeout ${PERF_TIMEOUT}s to guard the perf run"
timeout "${PERF_TIMEOUT}s" $MVN -B -ntp -Pperf-test \
  -Djmeter.skip=false \
  -Djmeter.base.dir="$JMETER_BASE_DIR" \
  -Dgw.host="$GW_HOST" \
  -Dgw.http.port="$GW_HTTP_PORT" \
  -Dgw.syslog.port="$GW_SYSLOG_PORT" \
  -Dusers="$USERS" \
  -DrampUp="$RAMP_UP" \
  -DdurationSeconds="$DURATION" \
  jmeter:configure jmeter:jmeter jmeter:results

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

REPORT_DIR="$JMETER_BASE_DIR"
echo "\n==> Done. Key outputs:"
echo "- HTML dashboards: $REPORT_DIR/reports/<plan>/index.html"
echo "- Raw CSV results: $REPORT_DIR/results/"
echo "- Perf summary:    $REPORT_DIR/perf-summary.md (and JSON if generated)"
echo "- App log:         $APP_LOG"

echo "==> Tip: open the main search report if available:"
MAIN_REPORT=$(ls -1 "$REPORT_DIR"/reports 2>/dev/null | head -n1 || true)
if [[ -n "$MAIN_REPORT" ]]; then
  if [[ "$REPORT_DIR" == "$ROOT_DIR"/* ]]; then
    echo "   file://$REPORT_DIR/reports/$MAIN_REPORT/index.html"
  else
    echo "   $REPORT_DIR/reports/$MAIN_REPORT/index.html"
  fi
fi

exit 0
