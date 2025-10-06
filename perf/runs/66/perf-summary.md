# GrepWise Performance Summary
- Run: 66  Commit: `056371c`  Branch: `main`  Time: 2025-10-06T00:42:45Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 2.0 | - | 1.1 | 919.44 | 0.00 | ✅ no baseline |
| HTTP Search | 1.0 | - | 0.8 | 1302.96 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.3 | 2947.07 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 1.1 | 2.0 | 475006 | 0.00 |
| Search GWPERF token | 0.4 | 1.0 | 156591 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23369 | 0.00 |
| GET /api/logs/search | 0.8 | 1.0 | 797 | 0.00 |
| GET /api/logs/count | 0.8 | 1.0 | 790 | 0.00 |

