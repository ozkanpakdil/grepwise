# GrepWise Performance Summary
- Run: 33  Commit: `1d93994`  Branch: `main`  Time: 2025-10-03T09:58:59Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 6.0 | - | 2.3 | 427.71 | 0.00 | ✅ no baseline |
| HTTP Search | 2.0 | - | 1.2 | 848.16 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.6 | 1635.98 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 2.5 | 6.0 | 217277 | 0.00 |
| Search GWPERF token | 0.7 | 1.0 | 86285 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23254 | 0.00 |
| GET /api/logs/search | 1.2 | 2.0 | 798 | 0.00 |
| GET /api/logs/count | 1.2 | 2.0 | 794 | 0.00 |

