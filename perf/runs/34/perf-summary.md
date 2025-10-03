# GrepWise Performance Summary
- Run: 34  Commit: `40454b1`  Branch: `main`  Time: 2025-10-03T11:20:29Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 6.0 | - | 2.4 | 421.58 | 0.00 | ✅ no baseline |
| HTTP Search | 2.0 | - | 1.4 | 694.87 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.6 | 1576.54 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 2.5 | 6.0 | 214012 | 0.00 |
| Search GWPERF token | 0.7 | 1.0 | 83231 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23142 | 0.00 |
| GET /api/logs/search | 1.5 | 2.0 | 797 | 0.00 |
| GET /api/logs/count | 1.4 | 2.0 | 788 | 0.00 |

