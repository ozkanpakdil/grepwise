# Playwright Performance Tracking

## Overview

Automatic performance tracking for Playwright tests. Every test run is recorded with timing data, and trends are calculated to detect performance regressions.

## Features

✅ **Automatic Tracking** - Runs after every `npm test`
✅ **Trend Detection** - Compares current run vs previous run
✅ **Averages** - Shows rolling average of last 10 runs
✅ **Per-Test Metrics** - Tracks individual test performance
✅ **Zero Cost** - Stores data locally in JSON file
✅ **CI Integration** - Works in GitHub Actions

## How It Works

1. **JSON Reporter** - Playwright outputs `test-results.json` after each run
2. **Post-Test Hook** - `track-performance.js` runs automatically via npm posttest
3. **History Storage** - Data saved to `test-performance.json` (last 100 runs)
4. **Trend Calculation** - Compares with previous run and calculates averages

## Output Example

```
═══════════════════════════════════════════════════════════
📊 PLAYWRIGHT PERFORMANCE REPORT
═══════════════════════════════════════════════════════════

⏱️  Total Duration: 5710ms (5.71s)
✅ Passed: 2
❌ Failed: 0
⏭️  Skipped: 40

📈 TRENDS (vs previous run):
───────────────────────────────────────────────────────────
🔴 SLOWER: +150ms (+2.7%)
📊 Average (last 10 runs): 5500ms

📋 Individual test trends:
───────────────────────────────────────────────────────────
🟢 authentication works - login, logout, invalid credentials
   Now: 5400ms | Before: 5650ms | Avg: 5500ms
   Δ -250ms (-4.4%)

═══════════════════════════════════════════════════════════

✅ Performance data saved (2 total runs tracked)
📁 History file: /home/ozkan/projects/grepwise/playwright/test-performance.json
```

## Performance Icons

- 🟢 **FASTER** - Test improved by >100ms
- 🔴 **SLOWER** - Test regressed by >100ms
- ➡️ **STABLE** - No significant change

## Configuration

```javascript
const PERF_FILE = 'test-performance.json';  // History file location
const MAX_HISTORY = 100;                     // Keep last 100 runs
const SIGNIFICANT_CHANGE_MS = 100;           // Threshold for flagging changes
```

## Viewing History

```bash
# View all runs
cat playwright/test-performance.json | jq '.runs'

# View latest run
cat playwright/test-performance.json | jq '.runs[-1]'

# Count total runs tracked
cat playwright/test-performance.json | jq '.runs | length'

# Get average duration over all runs
cat playwright/test-performance.json | jq '[.runs[].totalDuration] | add/length'
```

## CI Integration

Performance data is automatically uploaded to GitHub Actions artifacts:

```yaml
- name: Upload Playwright report and media
  uses: actions/upload-artifact@v4
  with:
    path: |
      playwright/playwright-report/
      playwright/test-results/
      playwright/test-performance.json  # ← Performance history
```

## Detecting Regressions

The script automatically flags significant changes:

- **Total time change >50ms** - Shows overall trend
- **Per-test change >100ms** - Shows which tests got slower/faster

Example regression detected:

```
🔴 SLOWER: +350ms (+6.2%)

🔴 authentication works - login, logout, invalid credentials
   Now: 6000ms | Before: 5650ms | Avg: 5500ms
   Δ +350ms (+6.2%)
```

## Best Practices

1. **Commit the JSON file** - Track history in git for long-term trends
2. **Monitor CI runs** - Check performance report in GitHub Actions logs
3. **Set up alerts** - Add workflow step to fail if regression >X%
4. **Investigate spikes** - Use videos/screenshots to debug slow tests

## Cost

**$0** - All data stored locally in JSON file. No external services required.

## Limitations

- Only tracks last 100 runs (configurable)
- No visualization dashboard (just console output)
- Requires JSON reporter to be enabled

## Future Enhancements

- Add HTML visualization dashboard
- Store history in GitHub Pages for long-term trends
- Slack/email alerts for regressions
- Compare against baseline/target times
