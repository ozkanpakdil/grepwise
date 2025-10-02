#!/usr/bin/env node
/**
 * Track Playwright test performance over time
 * Stores results in a JSON file and generates trend reports
 */

const fs = require('fs');
const path = require('path');

const PERF_FILE = path.join(__dirname, 'test-performance.json');
const MAX_HISTORY = 100; // Keep last 100 runs

function loadHistory() {
  if (!fs.existsSync(PERF_FILE)) {
    return { runs: [] };
  }
  return JSON.parse(fs.readFileSync(PERF_FILE, 'utf8'));
}

function saveHistory(data) {
  fs.writeFileSync(PERF_FILE, JSON.stringify(data, null, 2));
}

function parseTestResults() {
  const reportPath = path.join(__dirname, 'test-results.json');
  if (!fs.existsSync(reportPath)) {
    console.error('No test-results.json found. Make sure JSON reporter is enabled.');
    process.exit(1);
  }

  const results = JSON.parse(fs.readFileSync(reportPath, 'utf8'));
  const timestamp = new Date().toISOString();
  const gitSha = process.env.GITHUB_SHA || 'local';
  const runNumber = process.env.GITHUB_RUN_NUMBER || Date.now();

  const testResults = [];
  let totalDuration = 0;
  let passed = 0;
  let failed = 0;
  let skipped = 0;

  // Parse Playwright JSON reporter format
  function parseSpecs(specs) {
    specs.forEach(spec => {
      spec.tests?.forEach(test => {
        const result = test.results?.[0];
        if (!result) return;

        const duration = result.duration || 0;
        const status = test.expectedStatus === 'skipped' ? 'skipped' : result.status;

        testResults.push({
          name: spec.title,
          duration: Math.round(duration),
          status
        });

        totalDuration += duration;
        if (status === 'passed') passed++;
        else if (status === 'failed') failed++;
        else if (status === 'skipped') skipped++;
      });
    });
  }

  // Recursively parse suites
  function parseSuites(suites) {
    suites?.forEach(suite => {
      if (suite.specs) parseSpecs(suite.specs);
      if (suite.suites) parseSuites(suite.suites);
    });
  }

  parseSuites(results.suites);

  return {
    timestamp,
    runNumber,
    gitSha,
    totalDuration: Math.round(totalDuration),
    tests: testResults,
    summary: { passed, failed, skipped, total: passed + failed + skipped }
  };
}

function calculateTrends(history) {
  if (history.runs.length < 2) {
    return { message: 'Not enough data for trends (need at least 2 runs)' };
  }

  const current = history.runs[history.runs.length - 1];
  const previous = history.runs[history.runs.length - 2];

  const totalChange = current.totalDuration - previous.totalDuration;
  const totalChangePercent = previous.totalDuration > 0
    ? ((totalChange / previous.totalDuration) * 100).toFixed(1)
    : '0.0';

  const testTrends = current.tests
    .filter(t => t.status === 'passed') // Only track passed tests
    .map(currentTest => {
      const prevTest = previous.tests.find(t => t.name === currentTest.name && t.status === 'passed');
      if (!prevTest) return null;

      const change = currentTest.duration - prevTest.duration;
      const changePercent = prevTest.duration > 0
        ? ((change / prevTest.duration) * 100).toFixed(1)
        : '0.0';

      return {
        name: currentTest.name,
        current: currentTest.duration,
        previous: prevTest.duration,
        change,
        changePercent,
        trend: change > 100 ? '🔴' : change < -100 ? '🟢' : '➡️'
      };
    }).filter(Boolean);

  // Calculate averages from last 10 runs
  const last10 = history.runs.slice(-10);
  const avgDuration = Math.round(
    last10.reduce((sum, run) => sum + run.totalDuration, 0) / last10.length
  );

  const testAverages = {};
  current.tests.forEach(test => {
    if (test.status !== 'passed') return;

    const durations = last10
      .flatMap(run => run.tests)
      .filter(t => t.name === test.name && t.status === 'passed')
      .map(t => t.duration);

    if (durations.length > 0) {
      testAverages[test.name] = Math.round(
        durations.reduce((sum, d) => sum + d, 0) / durations.length
      );
    }
  });

  return {
    totalChange,
    totalChangePercent,
    avgDuration,
    testTrends,
    testAverages
  };
}

function generateReport(runData, trends) {
  console.log('\n═══════════════════════════════════════════════════════════');
  console.log('📊 PLAYWRIGHT PERFORMANCE REPORT');
  console.log('═══════════════════════════════════════════════════════════\n');

  console.log(`⏱️  Total Duration: ${runData.totalDuration}ms (${(runData.totalDuration / 1000).toFixed(2)}s)`);
  console.log(`✅ Passed: ${runData.summary.passed}`);
  console.log(`❌ Failed: ${runData.summary.failed}`);
  console.log(`⏭️  Skipped: ${runData.summary.skipped}\n`);

  if (trends.message) {
    console.log(`ℹ️  ${trends.message}\n`);
    return;
  }

  console.log('📈 TRENDS (vs previous run):');
  console.log('───────────────────────────────────────────────────────────');

  if (Math.abs(trends.totalChange) > 50) {
    const icon = trends.totalChange > 0 ? '🔴 SLOWER' : '🟢 FASTER';
    const sign = trends.totalChange > 0 ? '+' : '';
    console.log(`${icon}: ${sign}${trends.totalChange}ms (${sign}${trends.totalChangePercent}%)`);
  } else {
    console.log('➡️  Total: No significant change');
  }

  console.log(`📊 Average (last 10 runs): ${trends.avgDuration}ms\n`);

  if (trends.testTrends.length > 0) {
    console.log('📋 Individual test trends:');
    console.log('───────────────────────────────────────────────────────────');

    trends.testTrends.forEach(test => {
      const avg = trends.testAverages[test.name] || test.current;
      const sign = test.change > 0 ? '+' : '';

      // Only show tests with significant changes (>100ms)
      if (Math.abs(test.change) > 100) {
        console.log(
          `${test.trend} ${test.name}\n` +
          `   Now: ${test.current}ms | Before: ${test.previous}ms | Avg: ${avg}ms\n` +
          `   Δ ${sign}${test.change}ms (${sign}${test.changePercent}%)\n`
        );
      }
    });
  }

  console.log('═══════════════════════════════════════════════════════════\n');
}

function main() {
  console.log('📥 Loading test results...');
  const runData = parseTestResults();

  console.log('📂 Loading history...');
  const history = loadHistory();

  console.log('💾 Saving run data...');
  history.runs.push(runData);

  // Keep only last MAX_HISTORY runs
  if (history.runs.length > MAX_HISTORY) {
    history.runs = history.runs.slice(-MAX_HISTORY);
  }

  saveHistory(history);

  console.log('📊 Calculating trends...');
  const trends = calculateTrends(history);

  generateReport(runData, trends);

  console.log(`✅ Performance data saved (${history.runs.length} total runs tracked)`);
  console.log(`📁 History file: ${PERF_FILE}\n`);
}

main();
