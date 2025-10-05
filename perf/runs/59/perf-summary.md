# GrepWise Performance Summary
- Run: 59  Commit: `7e7f5f6`  Branch: `main`  Time: 2025-10-05T20:46:02Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 3.0 | - | 1.1 | 892.33 | 0.00 | ✅ no baseline |
| HTTP Search | 2.0 | - | 1.0 | 1040.18 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.3 | 2954.14 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 1.2 | 3.0 | 461127 | 0.00 |
| Search GWPERF token | 0.4 | 1.0 | 156838 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23294 | 0.00 |
| GET /api/logs/search | 1.0 | 2.0 | 792 | 0.00 |
| GET /api/logs/count | 0.9 | 2.0 | 787 | 0.00 |

