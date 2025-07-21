package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.service.SystemResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for system resource monitoring and evaluation.
 * Provides endpoints to evaluate system resource usage under various conditions.
 */
@RestController
@RequestMapping("/api/system/resources")
@Tag(name = "System Resources", description = "API for monitoring and evaluating system resource usage")
public class SystemResourceController {

    private final SystemResourceService systemResourceService;

    @Autowired
    public SystemResourceController(SystemResourceService systemResourceService) {
        this.systemResourceService = systemResourceService;
    }

    /**
     * Get current system metrics
     */
    @GetMapping("/metrics")
    @Operation(
            summary = "Get current system metrics",
            description = "Returns a snapshot of current system resource metrics including CPU, memory, and thread usage",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved system metrics",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<Map<String, Object>> getCurrentMetrics() {
        return ResponseEntity.ok(systemResourceService.collectSystemMetrics());
    }

    /**
     * Evaluate system resource usage under CPU-intensive load
     */
    @PostMapping("/evaluate/cpu")
    @Operation(
            summary = "Evaluate CPU-intensive load",
            description = "Runs a CPU-intensive test and returns resource usage metrics",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully completed CPU load test",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<Map<String, Object>> evaluateCpuLoad(
            @Parameter(description = "Duration of the test in seconds", required = true)
            @RequestParam(defaultValue = "10") int duration,
            
            @Parameter(description = "Number of threads to use", required = true)
            @RequestParam(defaultValue = "4") int threads) {
        
        Map<String, Object> results = systemResourceService.evaluateCpuIntensiveLoad(duration, threads);
        Map<String, Object> analysis = systemResourceService.analyzeResourceUsage(results);
        
        // Combine results and analysis
        results.put("analysis", analysis);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Evaluate system resource usage under memory-intensive load
     */
    @PostMapping("/evaluate/memory")
    @Operation(
            summary = "Evaluate memory-intensive load",
            description = "Runs a memory-intensive test and returns resource usage metrics",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully completed memory load test",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<Map<String, Object>> evaluateMemoryLoad(
            @Parameter(description = "Duration of the test in seconds", required = true)
            @RequestParam(defaultValue = "10") int duration,
            
            @Parameter(description = "Amount of memory to allocate in MB", required = true)
            @RequestParam(defaultValue = "100") int memoryMB) {
        
        Map<String, Object> results = systemResourceService.evaluateMemoryIntensiveLoad(duration, memoryMB);
        Map<String, Object> analysis = systemResourceService.analyzeResourceUsage(results);
        
        // Combine results and analysis
        results.put("analysis", analysis);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Evaluate system resource usage under I/O-intensive load
     */
    @PostMapping("/evaluate/io")
    @Operation(
            summary = "Evaluate I/O-intensive load",
            description = "Runs an I/O-intensive test and returns resource usage metrics",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully completed I/O load test",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<Map<String, Object>> evaluateIoLoad(
            @Parameter(description = "Duration of the test in seconds", required = true)
            @RequestParam(defaultValue = "10") int duration,
            
            @Parameter(description = "Number of threads to use", required = true)
            @RequestParam(defaultValue = "4") int threads) {
        
        Map<String, Object> results = systemResourceService.evaluateIoIntensiveLoad(duration, threads);
        Map<String, Object> analysis = systemResourceService.analyzeResourceUsage(results);
        
        // Combine results and analysis
        results.put("analysis", analysis);
        
        return ResponseEntity.ok(results);
    }

    /**
     * Run a comprehensive system resource evaluation
     */
    @PostMapping("/evaluate/comprehensive")
    @Operation(
            summary = "Run comprehensive resource evaluation",
            description = "Runs a series of tests to evaluate system resources under various conditions",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully completed comprehensive evaluation",
                            content = @Content(mediaType = "application/json")
                    )
            }
    )
    public ResponseEntity<Map<String, Object>> runComprehensiveEvaluation(
            @Parameter(description = "Duration of each test in seconds", required = true)
            @RequestParam(defaultValue = "10") int duration) {
        
        // Initial metrics
        Map<String, Object> initialMetrics = systemResourceService.collectSystemMetrics();
        
        // Run CPU test
        Map<String, Object> cpuResults = systemResourceService.evaluateCpuIntensiveLoad(duration, 
                (int) initialMetrics.get("availableProcessors"));
        Map<String, Object> cpuAnalysis = systemResourceService.analyzeResourceUsage(cpuResults);
        
        // Run memory test (allocate 10% of max heap)
        long maxHeapMB = (long) initialMetrics.get("heapMemoryMax") / (1024 * 1024);
        int memoryToAllocate = (int) (maxHeapMB * 0.1);
        Map<String, Object> memoryResults = systemResourceService.evaluateMemoryIntensiveLoad(duration, 
                memoryToAllocate > 0 ? memoryToAllocate : 100);
        Map<String, Object> memoryAnalysis = systemResourceService.analyzeResourceUsage(memoryResults);
        
        // Run I/O test
        Map<String, Object> ioResults = systemResourceService.evaluateIoIntensiveLoad(duration, 2);
        Map<String, Object> ioAnalysis = systemResourceService.analyzeResourceUsage(ioResults);
        
        // Combine all results
        Map<String, Object> comprehensiveResults = Map.of(
                "initialMetrics", initialMetrics,
                "cpuTest", Map.of("results", cpuResults, "analysis", cpuAnalysis),
                "memoryTest", Map.of("results", memoryResults, "analysis", memoryAnalysis),
                "ioTest", Map.of("results", ioResults, "analysis", ioAnalysis)
        );
        
        return ResponseEntity.ok(comprehensiveResults);
    }
}