#!/usr/bin/env python3
import csv
import glob
import json
import math
import os
from pathlib import Path
from statistics import mean
from datetime import datetime

# Config
RESULTS_DIR = Path('target/jmeter/results')
OUT_DIR = Path('target/jmeter')
HISTORY_DIR = Path('docs/perf')
HISTORY_FILE = HISTORY_DIR / 'history.csv'
SUMMARY_JSON = OUT_DIR / 'perf-summary.json'
SUMMARY_MD = OUT_DIR / 'perf-summary.md'
BADGE_SVG = Path('docs/perf/badge.svg')

RUN_NUMBER = os.getenv('GITHUB_RUN_NUMBER', '')
SHA = os.getenv('GITHUB_SHA', '')[:7]
BRANCH = os.getenv('GITHUB_REF_NAME', '')
NOW_ISO = datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ')

# Thresholds (percent increase vs moving average p95 latency)
WARN_PCT = 5.0
ALERT_PCT = 10.0
BLOCK_PCT = 20.0


def percentile(values, p):
    if not values:
        return float('nan')
    values = sorted(values)
    k = (len(values) - 1) * (p / 100.0)
    f = math.floor(k)
    c = math.ceil(k)
    if f == c:
        return float(values[int(k)])
    d0 = values[int(f)] * (c - k)
    d1 = values[int(c)] * (k - f)
    return float(d0 + d1)


def read_jmeter_csv(path: Path):
    # JMeter CSV default headers include: timeStamp,elapsed,label,responseCode,success,bytes,grpThreads,allThreads,Latency,IdleTime,Connect
    samples = []
    with path.open(newline='', encoding='utf-8', errors='ignore') as f:
        reader = csv.DictReader(f)
        # guard for files without headers
        if reader.fieldnames is None:
            return []
        # normalize header keys
        keys = [k.strip() for k in reader.fieldnames]
        for row in reader:
            try:
                elapsed = float(row.get('elapsed') or row.get('Elapsed') or 0)
                success_str = row.get('success') or row.get('Success') or 'false'
                success = str(success_str).lower() == 'true'
                label = row.get('label') or row.get('Label') or 'unknown'
                samples.append({'elapsed': elapsed, 'success': success, 'label': label})
            except Exception:
                continue
    return samples


def aggregate_samples(samples):
    if not samples:
        return {'count': 0, 'avg_ms': float('nan'), 'p95_ms': float('nan'), 'throughput': 0.0, 'errors': 0, 'success': 0, 'err_rate': 0.0}
    el = [s['elapsed'] for s in samples]
    count = len(el)
    avg_ms = mean(el)
    p95_ms = percentile(el, 95)
    errors = sum(1 for s in samples if not s['success'])
    success = count - errors
    # infer duration from timestamps not available; approximate throughput by 1000/avg per thread; better: use elapsed mean to estimate total qps
    # Here throughput ~ count / (sum(elapsed)/1000)
    total_elapsed_sec = sum(el) / 1000.0
    throughput = (count / total_elapsed_sec) if total_elapsed_sec > 0 else 0.0
    err_rate = (errors / count) * 100.0 if count > 0 else 0.0
    return {'count': count, 'avg_ms': avg_ms, 'p95_ms': p95_ms, 'throughput': throughput, 'errors': errors, 'success': success, 'err_rate': err_rate}


def read_history():
    hist = []
    if HISTORY_FILE.exists():
        with HISTORY_FILE.open(newline='', encoding='utf-8', errors='ignore') as f:
            reader = csv.DictReader(f)
            for row in reader:
                try:
                    hist.append({
                        'timestamp': row['timestamp'],
                        'run': row.get('run', ''),
                        'commit': row.get('commit', ''),
                        'branch': row.get('branch', ''),
                        'scenario': row['scenario'],
                        'avg_ms': float(row['avg_ms']),
                        'p95_ms': float(row['p95_ms']),
                        'throughput': float(row['throughput']),
                        'err_rate': float(row.get('err_rate', 0.0))
                    })
                except Exception:
                    continue
    return hist


def moving_average(values, window=10):
    if not values:
        return float('nan')
    take = values[-window:] if len(values) > window else values
    return mean(take)


def ensure_dirs():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    HISTORY_DIR.mkdir(parents=True, exist_ok=True)


def make_badge(color: str, label: str):
    svg = f"""
<svg xmlns='http://www.w3.org/2000/svg' width='150' height='20'>
  <linearGradient id='b' x2='0' y2='100%'>
    <stop offset='0' stop-color='#bbb' stop-opacity='.1'/>
    <stop offset='1' stop-opacity='.1'/>
  </linearGradient>
  <mask id='a'>
    <rect width='150' height='20' rx='3' fill='#fff'/>
  </mask>
  <g mask='url(#a)'>
    <rect width='80' height='20' fill='#555'/>
    <rect x='80' width='70' height='20' fill='{color}'/>
    <rect width='150' height='20' fill='url(#b)'/>
  </g>
  <g fill='#fff' text-anchor='middle' font-family='Verdana,Geneva,DejaVu Sans,sans-serif' font-size='11'>
    <text x='40' y='14'>Perf</text>
    <text x='115' y='14'>{label}</text>
  </g>
</svg>
"""
    BADGE_SVG.write_text(svg, encoding='utf-8')


def main():
    ensure_dirs()
    result_files = sorted(glob.glob(str(RESULTS_DIR / '*.csv')))
    if not result_files:
        print('No JMeter CSV results found under', RESULTS_DIR)
        return 0

    scenarios = {}
    for f in result_files:
        path = Path(f)
        name = path.stem  # e.g., http-search or similar
        samples = read_jmeter_csv(path)
        agg = aggregate_samples(samples)
        scenarios[name] = agg

    history = read_history()

    # Prepare summary and evaluate against moving average per scenario using p95 latency as primary metric
    summary = {
        'generatedAt': NOW_ISO,
        'run': RUN_NUMBER,
        'commit': SHA,
        'branch': BRANCH,
        'thresholds': {
            'warnPct': WARN_PCT,
            'alertPct': ALERT_PCT,
            'blockPct': BLOCK_PCT
        },
        'scenarios': {},
        'overall': {}
    }

    md_lines = []
    md_lines.append('# GrepWise Performance Summary')
    md_lines.append(f'- Run: {RUN_NUMBER}  Commit: `{SHA}`  Branch: `{BRANCH}`  Time: {NOW_ISO}')
    md_lines.append('')
    md_lines.append('| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |')
    md_lines.append('|---|---:|---:|---:|---:|---:|:--:|')

    worst_level = 'green'

    # compute per-scenario
    for name, agg in scenarios.items():
        # historical values of same scenario
        hist_vals = [h for h in history if h['scenario'] == name and not math.isnan(h['p95_ms']) and h['p95_ms'] > 0]
        avg_p95 = moving_average([h['p95_ms'] for h in hist_vals], window=10)
        cur_p95 = agg['p95_ms']
        delta_pct = float('nan')
        level = 'green'
        emoji = '✅'
        note = ''
        if not math.isnan(avg_p95) and avg_p95 > 0 and not math.isnan(cur_p95):
            delta_pct = ((cur_p95 - avg_p95) / avg_p95) * 100.0
            if delta_pct > BLOCK_PCT:
                level = 'red'
                emoji = '🛑'
                note = 'BLOCK (>20%)'
            elif delta_pct > ALERT_PCT:
                level = 'red'
                emoji = '🔴'
                note = 'RED (>10%)'
            elif delta_pct > WARN_PCT:
                level = 'yellow'
                emoji = '🟡'
                note = 'YELLOW (>5%)'
            else:
                level = 'green'
                emoji = '✅'
                note = 'OK'
        else:
            note = 'no baseline'

        worst_level = (
            'red' if level == 'red' or worst_level == 'red' else
            'yellow' if level == 'yellow' or worst_level == 'yellow' else
            'green'
        )

        summary['scenarios'][name] = {
            'avg_ms': agg['avg_ms'],
            'p95_ms': agg['p95_ms'],
            'throughput': agg['throughput'],
            'err_rate': agg['err_rate'],
            'baseline_p95_ms': avg_p95,
            'delta_pct_vs_baseline_p95': delta_pct,
            'level': level,
            'note': note
        }

        md_lines.append(f"| {name} | {agg['p95_ms']:.1f} | {delta_pct:.1f}% | {avg_p95 if not math.isnan(avg_p95) else float('nan'):.1f} | {agg['throughput']:.2f} | {agg['err_rate']:.2f} | {emoji} {note} |")

    # overall choose worst scenario level
    badge_color = {'green': '#4c1', 'yellow': '#dfb317', 'red': '#e05d44'}[worst_level]
    badge_text = {'green': 'OK', 'yellow': 'WARN', 'red': 'ALERT'}[worst_level]
    make_badge(badge_color, badge_text)

    summary['overall'] = {'level': worst_level, 'badge': badge_text}

    # write summary files
    SUMMARY_JSON.write_text(json.dumps(summary, indent=2), encoding='utf-8')
    SUMMARY_MD.write_text('\n'.join(md_lines) + '\n', encoding='utf-8')

    # append to history
    new_rows = []
    for name, agg in scenarios.items():
        new_rows.append({
            'timestamp': NOW_ISO,
            'run': RUN_NUMBER,
            'commit': SHA,
            'branch': BRANCH,
            'scenario': name,
            'avg_ms': f"{agg['avg_ms']:.3f}",
            'p95_ms': f"{agg['p95_ms']:.3f}",
            'throughput': f"{agg['throughput']:.5f}",
            'err_rate': f"{agg['err_rate']:.3f}"
        })

    # ensure header
    header = ['timestamp', 'run', 'commit', 'branch', 'scenario', 'avg_ms', 'p95_ms', 'throughput', 'err_rate']

    if not HISTORY_FILE.exists():
        with HISTORY_FILE.open('w', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=header)
            writer.writeheader()
            for r in new_rows:
                writer.writerow(r)
    else:
        with HISTORY_FILE.open('a', newline='', encoding='utf-8') as f:
            writer = csv.DictWriter(f, fieldnames=header)
            for r in new_rows:
                writer.writerow(r)

    # Print a compact line for logs
    print(json.dumps({
        'overall': summary['overall'],
        'scenarios': {k: {'p95_ms': v['p95_ms'], 'delta_pct': v['delta_pct_vs_baseline_p95'], 'level': v['level']} for k, v in summary['scenarios'].items()}
    }))

    # Determine exit code policy: block only if any scenario exceeds BLOCK_PCT increase
    should_block = any(v['level'] == 'red' and 'BLOCK' in v['note'] for v in summary['scenarios'].values())
    return 2 if should_block else 0


if __name__ == '__main__':
    raise SystemExit(main())
