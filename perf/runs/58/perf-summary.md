# GrepWise Performance Summary
- Run: 58  Commit: `4dbd228`  Branch: `main`  Time: 2025-10-05T20:15:47Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 2.0 | - | 1.1 | 901.21 | 0.00 | ✅ no baseline |
| HTTP Search | 2.0 | - | 0.9 | 1163.83 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.3 | 2895.60 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 1.1 | 3.0 | 464789 | 0.00 |
| Search GWPERF token | 0.4 | 1.0 | 153209 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23229 | 0.00 |
| GET /api/logs/search | 0.9 | 2.0 | 788 | 0.00 |
| GET /api/logs/count | 0.8 | 2.0 | 782 | 0.00 |

