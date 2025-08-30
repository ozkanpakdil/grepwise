package io.github.ozkanpakdil.grepwise.grpc;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
import io.github.ozkanpakdil.grepwise.service.SearchCacheService;
import io.github.ozkanpakdil.grepwise.service.SplQueryService;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * gRPC implementation of the SearchService.
 * This service provides advanced log searching and analytics capabilities via gRPC.
 */
@Service
public class SearchServiceGrpcImpl extends SearchServiceGrpc.SearchServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(SearchServiceGrpcImpl.class);

    private final LuceneService luceneService;
    private final SearchCacheService searchCacheService;
    private final SplQueryService splQueryService;

    @Autowired
    public SearchServiceGrpcImpl(LuceneService luceneService, SearchCacheService searchCacheService, SplQueryService splQueryService) {
        this.luceneService = luceneService;
        this.searchCacheService = searchCacheService;
        this.splQueryService = splQueryService;
    }

    @Override
    public void searchLogs(SearchLogsRequest request, StreamObserver<SearchLogsResponse> responseObserver) {
        try {
            List<LogEntry> logs = luceneService.search(
                    request.getQuery(),
                    request.getIsRegex(),
                    request.getStartTime(),
                    request.getEndTime()
            );

            SearchLogsResponse response = SearchLogsResponse.newBuilder()
                    .addAllLogs(convertToGrpcLogEntries(logs))
                    .setTotalCount(logs.size())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in searchLogs: ", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void executeSplQuery(SplQueryRequest request, StreamObserver<SplQueryResponse> responseObserver) {
        try {
            Object result = splQueryService.executeSplQuery(request.getSplQuery());
            SplQueryResponse.Builder responseBuilder = SplQueryResponse.newBuilder();

            if (result instanceof List && !((List<?>) result).isEmpty() && ((List<?>) result).get(0) instanceof LogEntry) {
                @SuppressWarnings("unchecked")
                List<LogEntry> logs = (List<LogEntry>) result;

                SearchLogsResponse logsResponse = SearchLogsResponse.newBuilder()
                        .addAllLogs(convertToGrpcLogEntries(logs))
                        .setTotalCount(logs.size())
                        .build();

                responseBuilder.setLogs(logsResponse);
            } else if (result instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> statsResult = (Map<String, Object>) result;

                if (statsResult.containsKey("columns") && statsResult.containsKey("rows")) {
                    @SuppressWarnings("unchecked")
                    List<String> columns = (List<String>) statsResult.get("columns");
                    @SuppressWarnings("unchecked")
                    List<List<String>> rows = (List<List<String>>) statsResult.get("rows");

                    StatisticsResult.Builder statsBuilder = StatisticsResult.newBuilder()
                            .addAllColumnNames(columns);

                    for (List<String> row : rows) {
                        StatisticsRow.Builder rowBuilder = StatisticsRow.newBuilder();
                        rowBuilder.addAllValues(row);
                        statsBuilder.addRows(rowBuilder.build());
                    }

                    responseBuilder.setStatistics(statsBuilder.build());
                } else {
                    responseBuilder.setErrorMessage("Unsupported statistics result format");
                }
            } else {
                responseBuilder.setErrorMessage("Unsupported result type: " +
                        (result != null ? result.getClass().getName() : "null"));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in executeSplQuery: ", e);
            SplQueryResponse response = SplQueryResponse.newBuilder()
                    .setErrorMessage("Error executing SPL query: " + e.getMessage())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getLogCountByTimeSlots(LogCountRequest request, StreamObserver<LogCountResponse> responseObserver) {
        try {
            // Calculate time range if a predefined range is specified
            Long startTime = request.getStartTime();
            Long endTime = request.getEndTime();
            String timeRange = request.getTimeRange();

            if (timeRange != null && !timeRange.equals("custom")) {
                long now = System.currentTimeMillis();
                long hours = 0;

                switch (timeRange) {
                    case "1h":
                        hours = 1;
                        break;
                    case "3h":
                        hours = 3;
                        break;
                    case "12h":
                        hours = 12;
                        break;
                    case "24h":
                        hours = 24;
                        break;
                    default:
                        // Invalid time range, ignore
                        break;
                }

                if (hours > 0) {
                    endTime = now;
                    startTime = now - (hours * 60 * 60 * 1000); // Convert hours to milliseconds
                }
            }

            // Default to last 24 hours if no time range is specified
            if (startTime == null || endTime == null) {
                endTime = System.currentTimeMillis();
                startTime = endTime - (24 * 60 * 60 * 1000); // 24 hours
            }

            // Get logs matching the query and time range
            List<LogEntry> logs = luceneService.search(request.getQuery(), request.getIsRegex(), startTime, endTime);

            // Calculate the size of each time slot
            int slots = request.getSlots();
            long timeRangeMs = endTime - startTime;
            long slotSizeMs = timeRangeMs / slots;

            // Initialize the result map with all slots (even empty ones)
            Map<Long, Integer> result = new TreeMap<>();
            for (int i = 0; i < slots; i++) {
                long slotStartTime = startTime + (i * slotSizeMs);
                result.put(slotStartTime, 0);
            }

            // Count logs in each time slot
            for (LogEntry log : logs) {
                // Use record time if available, otherwise use entry time
                long timeToCheck = log.recordTime() != null ? log.recordTime() : log.timestamp();

                // Find which slot this log belongs to
                int slotIndex = (int) ((timeToCheck - startTime) / slotSizeMs);

                // Ensure the slot index is valid
                if (slotIndex >= 0 && slotIndex < slots) {
                    long slotStartTime = startTime + (slotIndex * slotSizeMs);
                    result.put(slotStartTime, result.get(slotStartTime) + 1);
                }
            }

            LogCountResponse response = LogCountResponse.newBuilder()
                    .putAllCounts(result)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getLogCountByTimeSlots: ", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getLogsByLevel(LogLevelRequest request, StreamObserver<SearchLogsResponse> responseObserver) {
        try {
            List<LogEntry> logs = luceneService.findByLevel(request.getLevel());

            SearchLogsResponse response = SearchLogsResponse.newBuilder()
                    .addAllLogs(convertToGrpcLogEntries(logs))
                    .setTotalCount(logs.size())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getLogsByLevel: ", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getLogsBySource(LogSourceRequest request, StreamObserver<SearchLogsResponse> responseObserver) {
        try {
            List<LogEntry> logs = luceneService.findBySource(request.getSource());

            SearchLogsResponse response = SearchLogsResponse.newBuilder()
                    .addAllLogs(convertToGrpcLogEntries(logs))
                    .setTotalCount(logs.size())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getLogsBySource: ", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getLogsByTimeRange(TimeRangeRequest request, StreamObserver<SearchLogsResponse> responseObserver) {
        try {
            List<LogEntry> logs = luceneService.search(
                    "*",
                    false,
                    request.getStartTime(),
                    request.getEndTime()
            );

            SearchLogsResponse response = SearchLogsResponse.newBuilder()
                    .addAllLogs(convertToGrpcLogEntries(logs))
                    .setTotalCount(logs.size())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getLogsByTimeRange: ", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getLogLevels(EmptyRequest request, StreamObserver<LogLevelsResponse> responseObserver) {
        try {
            // Get all logs and extract distinct levels
            List<String> levels = luceneService.search(null, false, null, null).stream()
                    .map(LogEntry::level)
                    .distinct()
                    .collect(Collectors.toList());

            LogLevelsResponse response = LogLevelsResponse.newBuilder()
                    .addAllLevels(levels)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getLogLevels: ", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getLogSources(EmptyRequest request, StreamObserver<LogSourcesResponse> responseObserver) {
        try {
            // Get all logs and extract distinct sources
            List<String> sources = luceneService.search(null, false, null, null).stream()
                    .map(LogEntry::source)
                    .distinct()
                    .collect(Collectors.toList());

            LogSourcesResponse response = LogSourcesResponse.newBuilder()
                    .addAllSources(sources)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getLogSources: ", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void getCacheStats(EmptyRequest request, StreamObserver<CacheStatsResponse> responseObserver) {
        try {
            Map<String, Object> stats = searchCacheService.getCacheStats();

            CacheStatsResponse.Builder responseBuilder = CacheStatsResponse.newBuilder()
                    .setCacheSize((Integer) stats.getOrDefault("cacheSize", 0))
                    .setMaxCacheSize((Integer) stats.getOrDefault("maxCacheSize", 0))
                    .setExpirationMs((Integer) stats.getOrDefault("expirationMs", 0))
                    .setCacheEnabled((Boolean) stats.getOrDefault("cacheEnabled", false))
                    .setHits((Integer) stats.getOrDefault("hits", 0))
                    .setMisses((Integer) stats.getOrDefault("misses", 0));

            if (stats.containsKey("hitRatio")) {
                responseBuilder.setHitRatio((Double) stats.get("hitRatio"));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in getCacheStats: ", e);
            responseObserver.onError(e);
        }
    }

    @Override
    public void clearCache(EmptyRequest request, StreamObserver<CacheOperationResponse> responseObserver) {
        try {
            searchCacheService.clearCache();

            CacheOperationResponse response = CacheOperationResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Cache cleared successfully")
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in clearCache: ", e);
            CacheOperationResponse response = CacheOperationResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to clear cache: " + e.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void updateCacheConfig(CacheConfigRequest request, StreamObserver<CacheStatsResponse> responseObserver) {
        try {
            // Check if fields are set by comparing with default values
            if (request.getCacheEnabled()) {
                searchCacheService.setCacheEnabled(request.getCacheEnabled());
            }

            if (request.getMaxCacheSize() > 0) {
                searchCacheService.setMaxCacheSize(request.getMaxCacheSize());
            }

            if (request.getExpirationMs() > 0) {
                searchCacheService.setExpirationMs(request.getExpirationMs());
            }

            Map<String, Object> stats = searchCacheService.getCacheStats();

            CacheStatsResponse.Builder responseBuilder = CacheStatsResponse.newBuilder()
                    .setCacheSize((Integer) stats.getOrDefault("cacheSize", 0))
                    .setMaxCacheSize((Integer) stats.getOrDefault("maxCacheSize", 0))
                    .setExpirationMs((Integer) stats.getOrDefault("expirationMs", 0))
                    .setCacheEnabled((Boolean) stats.getOrDefault("cacheEnabled", false))
                    .setHits((Integer) stats.getOrDefault("hits", 0))
                    .setMisses((Integer) stats.getOrDefault("misses", 0));

            if (stats.containsKey("hitRatio")) {
                responseBuilder.setHitRatio((Double) stats.get("hitRatio"));
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            logger.error("Error in updateCacheConfig: ", e);
            responseObserver.onError(e);
        }
    }

    /**
     * Converts a list of model LogEntry objects to gRPC LogEntry messages.
     */
    private List<io.github.ozkanpakdil.grepwise.grpc.LogEntry> convertToGrpcLogEntries(List<io.github.ozkanpakdil.grepwise.model.LogEntry> logs) {
        return logs.stream()
                .map(this::convertToGrpcLogEntry)
                .collect(Collectors.toList());
    }

    /**
     * Converts a model LogEntry object to a gRPC LogEntry message.
     */
    private io.github.ozkanpakdil.grepwise.grpc.LogEntry convertToGrpcLogEntry(io.github.ozkanpakdil.grepwise.model.LogEntry log) {
        io.github.ozkanpakdil.grepwise.grpc.LogEntry.Builder builder = io.github.ozkanpakdil.grepwise.grpc.LogEntry.newBuilder()
                .setId(log.id() != null ? log.id() : "")
                .setTimestamp(log.timestamp())
                .setLevel(log.level() != null ? log.level() : "")
                .setMessage(log.message() != null ? log.message() : "")
                .setSource(log.source() != null ? log.source() : "");

        if (log.rawContent() != null) {
            builder.setRawContent(log.rawContent());
        }

        if (log.metadata() != null) {
            builder.putAllMetadata(log.metadata());
        }

        return builder.build();
    }
}