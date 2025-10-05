# GrepWise Performance Summary
- Run: 61  Commit: `bfae9f1`  Branch: `main`  Time: 2025-10-05T21:52:26Z

| Scenario | p95 (ms) | Δ vs avg | Avg (ms) | Throughput (req/s) | Errors (%) | Status |
|---|---:|---:|---:|---:|---:|:--:|
| Combined Parallel (BSH) | 3.0 | - | 1.1 | 890.39 | 0.00 | ✅ no baseline |
| HTTP Search | 1.0 | - | 0.8 | 1278.54 | 0.00 | ✅ no baseline |
| Syslog UDP | 1.0 | - | 0.3 | 2948.48 | 0.00 | ✅ no baseline |

### About scenarios

- HTTP Search: exercises the REST search endpoints over HTTP.
- Syslog UDP: sends log events into the UDP syslog ingestion path.
- Combined Parallel (BSH): mixed workload that runs multiple samplers in parallel using a BeanShell controller/sampler.

## Endpoints tested (by JMeter label)

| Endpoint | Avg (ms) | p95 (ms) | Count | Errors (%) |
|---|---:|---:|---:|---:|
| GET /api/logs/search GWPERF-PARALLEL | 1.2 | 3.0 | 460389 | 0.00 |
| Search GWPERF token | 0.4 | 1.0 | 155684 | 0.00 |
| UDP Syslog Send (BeanShell) | 0.1 | 1.0 | 23282 | 0.00 |
| GET /api/logs/search | 0.8 | 1.0 | 792 | 0.00 |
| GET /api/logs/count | 0.8 | 1.0 | 787 | 0.00 |

