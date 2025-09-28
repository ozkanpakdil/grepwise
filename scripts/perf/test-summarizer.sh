#!/usr/bin/env bash
# Quick local tester for the Java performance summarizer (SummarizeAndCompare.java)
#
# It will:
# - Use existing JMeter CSV results under target/jmeter/results if present,
#   or generate small synthetic CSVs that mimic JMeter outputs.
# - Run the Java summarizer to produce Markdown/JSON summary, update history, and badge.
# - Print where to find the outputs and show a short preview.
#
# Usage:
#   chmod +x scripts/perf/test-summarizer.sh
#   scripts/perf/test-summarizer.sh
#
# Notes:
# - Requires Java 24+ (we recommend Java 25).
# - Does NOT start the app or run JMeter; it only tests the summarizer.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT_DIR"

RESULTS_DIR="target/jmeter/results"
OUT_DIR="target/jmeter"

mkdir -p "$RESULTS_DIR"

function have_csvs() {
  shopt -s nullglob
  local files=("$RESULTS_DIR"/*.csv)
  [[ ${#files[@]} -gt 0 ]]
}

if have_csvs; then
  echo "==> Found existing CSV results under $RESULTS_DIR. Using them as-is."
else
  echo "==> No CSV results found. Generating small synthetic CSVs for demo..."
  ts=$(date +%Y%m%d)
  # We only need columns: elapsed, success, label (timestamp optional)
  header="timeStamp,elapsed,label,success"

  gen_file() {
    local file="$1"; shift
    local label="$1"; shift
    local base_ms="$1"; shift
    local spread="$1"; shift
    echo "$header" > "$file"
    for i in $(seq 1 120); do
      # random elapsed around base +/- spread
      r=$(( RANDOM % (spread*2+1) )) || r=0
      elapsed=$(( base_ms - spread + r ))
      if (( elapsed < 1 )); then elapsed=1; fi
      echo "$(date +%s%3N),$elapsed,$label,true" >> "$file"
    done
  }

  gen_file "$RESULTS_DIR/${ts}-http-search.csv"         "GET /api/logs/search" 120 60
  gen_file "$RESULTS_DIR/${ts}-syslog-udp.csv"          "UDP Syslog Send"       40  20
  gen_file "$RESULTS_DIR/${ts}-combined-parallel-bsh.csv" "GWPERF-PARALLEL"     180 80
  echo "==> Synthetic CSVs created in $RESULTS_DIR"
fi

if [[ ! -f scripts/perf/SummarizeAndCompare.java ]]; then
  echo "ERROR: scripts/perf/SummarizeAndCompare.java not found." >&2
  exit 1
fi

echo "==> Running Java summarizer"
java scripts/perf/SummarizeAndCompare.java || true

echo "\n==> Outputs:"
if [[ -f "$OUT_DIR/perf-summary.md" ]]; then
  echo "- Markdown summary: $OUT_DIR/perf-summary.md"
  echo "- JSON summary:     $OUT_DIR/perf-summary.json"
else
  echo "WARN: Summary files not found in $OUT_DIR. Did the summarizer run?" >&2
fi
if [[ -f docs/perf/history.csv ]]; then
  echo "- History:          docs/perf/history.csv (appended)"
fi
if [[ -f docs/perf/badge.svg ]]; then
  echo "- Badge:            docs/perf/badge.svg"
fi

# Show a small preview (first table lines)
if [[ -f "$OUT_DIR/perf-summary.md" ]]; then
  echo "\n==> Preview (first 20 lines of perf-summary.md):"
  head -n 20 "$OUT_DIR/perf-summary.md" || true
fi

echo "\n==> Done. You can re-run this script to append more entries to history and see trend deltas."
