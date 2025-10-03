# GrepWise Performance Summary
- Run: 32  Commit: `dfe9614`  Branch: `main`  Time: 2025-10-03T09:28:47Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 6.0 | - | 2.3 | 426.29 | 0.00 | ✅ no baseline |
| HTTP Search | 2.0 | - | 1.2 | 867.96 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.6 | 1616.89 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 2.5 | 6.0 | 216308 | 0.00 |
| Search GWPERF token | 0.7 | 1.0 | 85415 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23292 | 0.00 |
| GET /api/logs/search | 1.2 | 2.0 | 788 | 0.00 |
| GET /api/logs/count | 1.1 | 2.0 | 783 | 0.00 |

