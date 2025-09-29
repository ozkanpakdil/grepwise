# GrepWise Performance Summary
- Run: 14  Commit: `bf51e3e`  Branch: `main`  Time: 2025-09-29T18:33:37Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 12.0 | - | 4.7 | 212.95 | 0.00 | ✅ no baseline |
| HTTP Search | 2.0 | - | 1.2 | 850.62 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.7 | 1466.75 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 5.4 | 12.0 | 101331 | 0.00 |
| Search GWPERF token | 0.7 | 1.0 | 75907 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23259 | 0.00 |
| GET /api/logs/search | 1.2 | 2.0 | 787 | 0.00 |
| GET /api/logs/count | 1.1 | 2.0 | 779 | 0.00 |

