# GrepWise Performance Summary
- Run: 63  Commit: `5e9bf9a`  Branch: `main`  Time: 2025-10-05T22:54:13Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 3.0 | - | 1.2 | 869.54 | 0.00 | ✅ no baseline |
| HTTP Search | 2.0 | - | 1.1 | 893.77 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.4 | 2791.07 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 1.2 | 3.0 | 449337 | 0.00 |
| Search GWPERF token | 0.4 | 1.0 | 147500 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23279 | 0.00 |
| GET /api/logs/search | 1.1 | 2.0 | 784 | 0.00 |
| GET /api/logs/count | 1.1 | 2.0 | 781 | 0.00 |

