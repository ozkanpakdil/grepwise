# Log Ingestion Benchmark Tool

This directory contains tools for benchmarking log ingestion performance in GrepWise. The benchmarks measure various aspects of log ingestion performance, including throughput, CPU usage, and memory consumption.

## Overview

The benchmark tools in this directory help measure and analyze the performance of log ingestion in GrepWise. They provide insights into how the system performs under different loads and configurations, which can help identify bottlenecks and optimize performance.

## Benchmark Tools

### LogIngestionBenchmarkTool

This is a standalone command-line tool for benchmarking log ingestion performance. It measures:

- Ingestion time (how long it takes to ingest logs)
- Throughput (logs per second)
- CPU usage during ingestion
- Memory usage (both heap and non-heap) during ingestion

The tool tests different scenarios:
- Small batch ingestion (100 logs)
- Medium batch ingestion (1000 logs)
- Large batch ingestion (5000 logs)
- Concurrent ingestion (multiple threads)

For each scenario, it compares direct ingestion (bypassing buffer) and buffered ingestion.

### LogIngestionBenchmark

This is a JUnit test class that provides similar functionality to the standalone tool but runs within the Spring context. It's useful for running benchmarks as part of the test suite.

## Running the Benchmarks

### Using the Standalone Tool

To run the standalone benchmark tool:

1. Build the project: `mvn clean package`
2. Run the tool: `java -cp target/grepwise.jar io.github.ozkanpakdil.grepwise.benchmark.LogIngestionBenchmarkTool`

### Using the JUnit Test

To run the benchmark as a JUnit test:

1. Run the test: `mvn test -Dtest=LogIngestionBenchmark`

## Benchmark Results

The benchmark results are saved to a CSV file named `benchmark-results.csv` in the project root directory. The file contains the following columns:

- Timestamp: When the benchmark was run
- Test: The name of the test (SmallBatch, MediumBatch, LargeBatch, ConcurrentIngestion)
- BatchSize: The number of logs in the batch
- Mode: The ingestion mode (Direct or Buffered)
- IngestionTime(ms): The time it took to ingest the logs in milliseconds
- LogsPerSecond: The throughput in logs per second
- CPUUsage(%): The CPU usage during ingestion as a percentage
- HeapMemoryUsed(MB): The heap memory used during ingestion in megabytes
- NonHeapMemoryUsed(MB): The non-heap memory used during ingestion in megabytes

## Analyzing Results

You can use the benchmark results to:

1. Compare the performance of direct vs. buffered ingestion
2. Analyze how ingestion performance scales with batch size
3. Identify potential bottlenecks in the ingestion pipeline
4. Monitor performance changes over time

## Extending the Benchmarks

To add new benchmark scenarios:

1. Modify the existing benchmark tools or create new ones
2. Add new test methods for the scenarios you want to benchmark
3. Update this README to document the new scenarios

## Best Practices

When running benchmarks:

1. Run them on a dedicated machine to minimize interference from other processes
2. Run them multiple times to get more reliable results
3. Warm up the JVM before measuring performance
4. Clear the Lucene index between runs to ensure consistent starting conditions
5. Monitor system resources during the benchmark to identify potential bottlenecks

## Troubleshooting

If you encounter issues with the benchmarks:

1. Check that you have sufficient memory allocated to the JVM
2. Ensure that the Lucene index directory is writable
3. Check the logs for any error messages
4. Try running with a smaller batch size if you're running out of memory