# GrepWise Performance Summary
- Run: 55  Commit: `470cd1d`  Branch: `main`  Time: 2025-10-04T08:38:42Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 2.0 | - | 0.9 | 1176.26 | 97.37 | ✅ no baseline |
| HTTP Search | 2.0 | - | 0.8 | 1290.48 | 69.29 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.3 | 3890.17 | 96.32 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 0.9 | 2.0 | 606897 | 99.93 |
| Search GWPERF token | 0.3 | 1.0 | 202889 | 99.85 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23395 | 0.00 |
| GET /api/logs/search | 0.8 | 2.0 | 796 | 68.84 |
| GET /api/logs/count | 0.7 | 2.0 | 790 | 69.75 |

