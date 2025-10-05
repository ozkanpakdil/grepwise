# GrepWise Performance Summary
- Run: 62  Commit: `228cc31`  Branch: `main`  Time: 2025-10-05T22:20:19Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 2.0 | - | 1.1 | 898.83 | 0.00 | ✅ no baseline |
| HTTP Search | 1.0 | - | 0.7 | 1377.16 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.3 | 2948.58 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 1.1 | 3.0 | 464262 | 0.00 |
| Search GWPERF token | 0.4 | 1.0 | 156285 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23285 | 0.00 |
| GET /api/logs/search | 0.7 | 1.0 | 799 | 0.00 |
| GET /api/logs/count | 0.7 | 1.0 | 793 | 0.00 |

