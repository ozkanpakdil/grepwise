package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.grpc.*;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.LogRepository;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service implementation for handling log operations.
 */
@Service
public class LogService extends LogServiceGrpc.LogServiceImplBase {

    private final LogRepository logRepository;

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    public StreamObserver<io.github.ozkanpakdil.grepwise.grpc.LogEntry> streamLogs(StreamObserver<LogResponse> responseObserver) {
        return new StreamObserver<>() {
            private int processedCount = 0;

            @Override
            public void onNext(io.github.ozkanpakdil.grepwise.grpc.LogEntry logEntry) {
                // Convert gRPC LogEntry to model LogEntry
                LogEntry modelLogEntry = convertToModelLogEntry(logEntry);
                logRepository.save(modelLogEntry);
                processedCount++;
            }

            @Override
            public void onError(Throwable throwable) {
                responseObserver.onNext(LogResponse.newBuilder()
                        .setProcessedCount(processedCount)
                        .setSuccess(false)
                        .setMessage("Error processing logs: " + throwable.getMessage())
                        .build());
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(LogResponse.newBuilder()
                        .setProcessedCount(processedCount)
                        .setSuccess(true)
                        .setMessage("Successfully processed " + processedCount + " logs")
                        .build());
                responseObserver.onCompleted();
            }
        };
    }

    @Override
    public void searchLogs(SearchRequest request, StreamObserver<SearchResponse> responseObserver) {
        List<LogEntry> logs;
        
        // Apply time range filter if provided
        if (request.getStartTime() > 0 && request.getEndTime() > 0) {
            logs = logRepository.findByTimeRange(request.getStartTime(), request.getEndTime());
        } else {
            logs = logRepository.findAll();
        }
        
        // Apply query filter if provided
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            logs = logs.stream()
                    .filter(log -> log.getMessage() != null && 
                            log.getMessage().toLowerCase().contains(request.getQuery().toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // Apply sorting if provided
        if (request.getSortField() != null && !request.getSortField().isEmpty()) {
            logs = sortLogs(logs, request.getSortField(), request.getSortAscending());
        }
        
        // Apply pagination
        int totalResults = logs.size();
        int totalPages = (int) Math.ceil((double) totalResults / request.getSize());
        int page = Math.max(0, Math.min(request.getPage(), totalPages - 1));
        int fromIndex = page * request.getSize();
        int toIndex = Math.min(fromIndex + request.getSize(), totalResults);
        
        if (fromIndex < totalResults) {
            logs = logs.subList(fromIndex, toIndex);
        } else {
            logs = List.of();
        }
        
        // Convert model LogEntry to gRPC LogEntry
        List<io.github.ozkanpakdil.grepwise.grpc.LogEntry> grpcLogs = logs.stream()
                .map(this::convertToGrpcLogEntry)
                .collect(Collectors.toList());
        
        SearchResponse response = SearchResponse.newBuilder()
                .addAllLogs(grpcLogs)
                .setTotalPages(totalPages)
                .setCurrentPage(page)
                .setTotalResults(totalResults)
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getLogById(LogIdRequest request, StreamObserver<io.github.ozkanpakdil.grepwise.grpc.LogEntry> responseObserver) {
        LogEntry logEntry = logRepository.findById(request.getId());
        if (logEntry != null) {
            responseObserver.onNext(convertToGrpcLogEntry(logEntry));
        } else {
            // Return an empty log entry if not found
            responseObserver.onNext(io.github.ozkanpakdil.grepwise.grpc.LogEntry.newBuilder().build());
        }
        responseObserver.onCompleted();
    }

    @Override
    public void deleteLogs(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        List<LogEntry> logs;
        
        // Apply time range filter if provided
        if (request.getStartTime() > 0 && request.getEndTime() > 0) {
            logs = logRepository.findByTimeRange(request.getStartTime(), request.getEndTime());
        } else {
            logs = logRepository.findAll();
        }
        
        // Apply query filter if provided
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            logs = logs.stream()
                    .filter(log -> log.getMessage() != null && 
                            log.getMessage().toLowerCase().contains(request.getQuery().toLowerCase()))
                    .collect(Collectors.toList());
        }
        
        // Delete the filtered logs
        int deletedCount = 0;
        for (LogEntry log : logs) {
            if (logRepository.deleteById(log.getId())) {
                deletedCount++;
            }
        }
        
        DeleteResponse response = DeleteResponse.newBuilder()
                .setDeletedCount(deletedCount)
                .setSuccess(true)
                .setMessage("Successfully deleted " + deletedCount + " logs")
                .build();
        
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    /**
     * Convert a gRPC LogEntry to a model LogEntry.
     *
     * @param grpcLogEntry The gRPC LogEntry to convert
     * @return The converted model LogEntry
     */
    private LogEntry convertToModelLogEntry(io.github.ozkanpakdil.grepwise.grpc.LogEntry grpcLogEntry) {
        Map<String, String> metadata = new HashMap<>(grpcLogEntry.getMetadataMap());
        
        return new LogEntry(
                grpcLogEntry.getId(),
                grpcLogEntry.getTimestamp(),
                grpcLogEntry.getLevel(),
                grpcLogEntry.getMessage(),
                grpcLogEntry.getSource(),
                metadata,
                grpcLogEntry.getRawContent()
        );
    }

    /**
     * Convert a model LogEntry to a gRPC LogEntry.
     *
     * @param modelLogEntry The model LogEntry to convert
     * @return The converted gRPC LogEntry
     */
    private io.github.ozkanpakdil.grepwise.grpc.LogEntry convertToGrpcLogEntry(LogEntry modelLogEntry) {
        io.github.ozkanpakdil.grepwise.grpc.LogEntry.Builder builder = io.github.ozkanpakdil.grepwise.grpc.LogEntry.newBuilder()
                .setId(modelLogEntry.getId())
                .setTimestamp(modelLogEntry.getTimestamp())
                .setLevel(modelLogEntry.getLevel())
                .setMessage(modelLogEntry.getMessage())
                .setSource(modelLogEntry.getSource())
                .setRawContent(modelLogEntry.getRawContent());
        
        if (modelLogEntry.getMetadata() != null) {
            builder.putAllMetadata(modelLogEntry.getMetadata());
        }
        
        return builder.build();
    }

    /**
     * Sort logs by the specified field.
     *
     * @param logs          The logs to sort
     * @param sortField     The field to sort by
     * @param sortAscending Whether to sort in ascending order
     * @return The sorted logs
     */
    private List<LogEntry> sortLogs(List<LogEntry> logs, String sortField, boolean sortAscending) {
        return logs.stream()
                .sorted((log1, log2) -> {
                    int result;
                    switch (sortField) {
                        case "timestamp":
                            result = Long.compare(log1.getTimestamp(), log2.getTimestamp());
                            break;
                        case "level":
                            result = log1.getLevel().compareTo(log2.getLevel());
                            break;
                        case "message":
                            result = log1.getMessage().compareTo(log2.getMessage());
                            break;
                        case "source":
                            result = log1.getSource().compareTo(log2.getSource());
                            break;
                        default:
                            result = 0;
                    }
                    return sortAscending ? result : -result;
                })
                .collect(Collectors.toList());
    }
}