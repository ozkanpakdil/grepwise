# GrepWise Performance Summary
- Run: 56  Commit: `c92a163`  Branch: `main`  Time: 2025-10-04T09:16:29Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 2.0 | - | 1.1 | 901.84 | 0.00 | ✅ no baseline |
| HTTP Search | 2.0 | - | 1.0 | 1010.82 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.3 | 2941.00 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 1.1 | 3.0 | 466010 | 0.00 |
| Search GWPERF token | 0.4 | 1.0 | 156249 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23286 | 0.00 |
| GET /api/logs/search | 1.0 | 2.0 | 797 | 0.00 |
| GET /api/logs/count | 1.0 | 2.0 | 791 | 0.00 |

