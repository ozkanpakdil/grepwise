package io.github.ozkanpakdil.grepwise.config;

import io.github.ozkanpakdil.grepwise.service.LogBufferService;
import io.github.ozkanpakdil.grepwise.service.LogScannerService;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;

/**
 * Configuration for custom metrics in the application.
 * Registers metrics for key services to monitor their performance and health.
 */
@Configuration
public class MetricsConfig {

    /**
     * Registers metrics for the Lucene service.
     */
    @Bean
    @ConditionalOnBean(LuceneService.class)
    public MeterBinder luceneMetrics(LuceneService luceneService) {
        return registry -> {
            // We can't directly access internal Lucene metrics, but we can expose
            // some basic metrics about the service

            // In a real application, you would add more metrics here based on
            // what's important to monitor for your specific use case
        };
    }

    /**
     * Registers metrics for the LogBuffer service.
     */
    @Bean
    @ConditionalOnBean(LogBufferService.class)
    public MeterBinder logBufferMetrics(LogBufferService logBufferService) {
        return registry -> {
            // Register a gauge for buffer size
            Gauge.builder("grepwise.logbuffer.size", logBufferService::getBufferSize)
                    .description("Current size of the log buffer")
                    .register(registry);

            // Register a gauge for buffer utilization
            Gauge.builder("grepwise.logbuffer.utilization",
                            () -> (double) logBufferService.getBufferSize() / logBufferService.getMaxBufferSize())
                    .description("Current utilization of the log buffer (0-1)")
                    .register(registry);

            // Register a gauge for max buffer size
            Gauge.builder("grepwise.logbuffer.max_size", logBufferService::getMaxBufferSize)
                    .description("Maximum size of the log buffer")
                    .register(registry);

            // Register a gauge for flush interval
            Gauge.builder("grepwise.logbuffer.flush_interval_ms", logBufferService::getFlushIntervalMs)
                    .description("Flush interval of the log buffer in milliseconds")
                    .register(registry);
        };
    }

    /**
     * Registers metrics for the LogScanner service.
     */
    @Bean
    @ConditionalOnBean(LogScannerService.class)
    public MeterBinder logScannerMetrics(LogScannerService logScannerService) {
        return registry -> {
            // Register a gauge for the number of configured log directories
            Gauge.builder("grepwise.logscanner.configured_directories",
                            () -> logScannerService.getAllConfigs().size())
                    .description("Number of configured log directories")
                    .register(registry);
        };
    }

    /**
     * Registers application-level metrics.
     */
    @Bean
    public MeterBinder applicationMetrics() {
        return registry -> {
            // Register JVM metrics
            // Spring Boot Actuator already includes JVM metrics by default

            // Register system metrics
            // Spring Boot Actuator already includes system metrics by default

            // Register custom system resource metrics
            java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

            // Basic OS metrics
            Gauge.builder("grepwise.system.load.average", osBean::getSystemLoadAverage)
                    .description("System load average")
                    .register(registry);

            Gauge.builder("grepwise.system.cpu.available", osBean::getAvailableProcessors)
                    .description("Number of available processors")
                    .register(registry);

            // Memory metrics
            Gauge.builder("grepwise.memory.heap.used",
                            () -> memoryBean.getHeapMemoryUsage().getUsed())
                    .description("Current heap memory used")
                    .baseUnit("bytes")
                    .register(registry);

            Gauge.builder("grepwise.memory.heap.max",
                            () -> memoryBean.getHeapMemoryUsage().getMax())
                    .description("Maximum heap memory")
                    .baseUnit("bytes")
                    .register(registry);

            Gauge.builder("grepwise.memory.nonheap.used",
                            () -> memoryBean.getNonHeapMemoryUsage().getUsed())
                    .description("Current non-heap memory used")
                    .baseUnit("bytes")
                    .register(registry);

            // Thread metrics
            Gauge.builder("grepwise.threads.count", threadBean::getThreadCount)
                    .description("Current thread count")
                    .register(registry);

            Gauge.builder("grepwise.threads.peak", threadBean::getPeakThreadCount)
                    .description("Peak thread count")
                    .register(registry);

            Gauge.builder("grepwise.threads.daemon", threadBean::getDaemonThreadCount)
                    .description("Daemon thread count")
                    .register(registry);

            // Extended metrics if available
            try {
                // Try to access com.sun.management.OperatingSystemMXBean
                Class<?> sunOsBeanClass = Class.forName("com.sun.management.OperatingSystemMXBean");
                if (sunOsBeanClass.isInstance(osBean)) {
                    // Use reflection to safely access the methods
                    Object sunOsBean = osBean;

                    try {
                        java.lang.reflect.Method getProcessCpuLoad = sunOsBeanClass.getMethod("getProcessCpuLoad");
                        Gauge.builder("grepwise.cpu.process.load",
                                        () -> {
                                            try {
                                                return (double) getProcessCpuLoad.invoke(sunOsBean);
                                            } catch (Exception e) {
                                                return -1.0;
                                            }
                                        })
                                .description("Process CPU load (0.0-1.0)")
                                .register(registry);
                    } catch (NoSuchMethodException e) {
                        // Method not available, skip this metric
                    }

                    try {
                        java.lang.reflect.Method getCpuLoad = sunOsBeanClass.getMethod("getCpuLoad");
                        Gauge.builder("grepwise.cpu.system.load",
                                        () -> {
                                            try {
                                                return (double) getCpuLoad.invoke(sunOsBean);
                                            } catch (Exception e) {
                                                return -1.0;
                                            }
                                        })
                                .description("System CPU load (0.0-1.0)")
                                .register(registry);
                    } catch (NoSuchMethodException e) {
                        // Method not available, skip this metric
                    }

                    try {
                        java.lang.reflect.Method getFreeMemorySize = sunOsBeanClass.getMethod("getFreeMemorySize");
                        Gauge.builder("grepwise.memory.physical.free",
                                        () -> {
                                            try {
                                                return (long) getFreeMemorySize.invoke(sunOsBean);
                                            } catch (Exception e) {
                                                return -1L;
                                            }
                                        })
                                .description("Free physical memory")
                                .baseUnit("bytes")
                                .register(registry);
                    } catch (NoSuchMethodException e) {
                        // Method not available, skip this metric
                    }

                    try {
                        java.lang.reflect.Method getTotalMemorySize = sunOsBeanClass.getMethod("getTotalMemorySize");
                        Gauge.builder("grepwise.memory.physical.total",
                                        () -> {
                                            try {
                                                return (long) getTotalMemorySize.invoke(sunOsBean);
                                            } catch (Exception e) {
                                                return -1L;
                                            }
                                        })
                                .description("Total physical memory")
                                .baseUnit("bytes")
                                .register(registry);
                    } catch (NoSuchMethodException e) {
                        // Method not available, skip this metric
                    }

                    try {
                        java.lang.reflect.Method getCommittedVirtualMemorySize = sunOsBeanClass.getMethod("getCommittedVirtualMemorySize");
                        Gauge.builder("grepwise.memory.virtual.committed",
                                        () -> {
                                            try {
                                                return (long) getCommittedVirtualMemorySize.invoke(sunOsBean);
                                            } catch (Exception e) {
                                                return -1L;
                                            }
                                        })
                                .description("Committed virtual memory")
                                .baseUnit("bytes")
                                .register(registry);
                    } catch (NoSuchMethodException e) {
                        // Method not available, skip this metric
                    }
                }
            } catch (ClassNotFoundException e) {
                // com.sun.management.OperatingSystemMXBean not available, skip extended metrics
            }
        };
    }
}