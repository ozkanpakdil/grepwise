# GrepWise Performance Summary
- Run: 61  Commit: `bfae9f1`  Branch: `main`  Time: 2025-10-05T21:43:42Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 3.0 | - | 1.2 | 860.39 | 0.00 | ✅ no baseline |
| HTTP Search | 1.0 | - | 0.8 | 1221.44 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.3 | 2861.53 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 1.2 | 3.0 | 443148 | 0.00 |
| Search GWPERF token | 0.4 | 1.0 | 151012 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23362 | 0.00 |
| GET /api/logs/search | 0.8 | 1.0 | 784 | 0.00 |
| GET /api/logs/count | 0.8 | 2.0 | 777 | 0.00 |

