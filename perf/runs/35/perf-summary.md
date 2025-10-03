# GrepWise Performance Summary
- Run: 35  Commit: `f8c07f1`  Branch: `main`  Time: 2025-10-03T14:29:20Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 6.0 | - | 2.3 | 430.30 | 0.00 | ✅ no baseline |
| HTTP Search | 2.0 | - | 1.2 | 805.30 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.6 | 1623.25 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 2.5 | 6.0 | 218593 | 0.00 |
| Search GWPERF token | 0.7 | 1.0 | 85689 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23333 | 0.00 |
| GET /api/logs/search | 1.3 | 2.0 | 793 | 0.00 |
| GET /api/logs/count | 1.2 | 2.0 | 787 | 0.00 |

