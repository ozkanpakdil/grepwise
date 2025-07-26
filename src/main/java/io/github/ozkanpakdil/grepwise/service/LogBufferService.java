package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for buffering log entries before indexing them.
 * This helps manage memory usage for high-volume log ingestion by processing logs in batches.
 */
@Service
public class LogBufferService {
    private static final Logger logger = LoggerFactory.getLogger(LogBufferService.class);

    private final LuceneService luceneService;
    private final ConcurrentLinkedQueue<LogEntry> buffer;
    private final AtomicInteger bufferSize;
    private final ReentrantLock flushLock;

    @Value("${grepwise.buffer.max-size:1000}")
    private int maxBufferSize;

    @Value("${grepwise.buffer.flush-interval-ms:30000}")
    private int flushIntervalMs;

    public LogBufferService(LuceneService luceneService) {
        this.luceneService = luceneService;
        this.buffer = new ConcurrentLinkedQueue<>();
        this.bufferSize = new AtomicInteger(0);
        this.flushLock = new ReentrantLock();
        logger.info("LogBufferService initialized");
    }

    @PostConstruct
    public void init() {
        logger.info("LogBufferService started with maxBufferSize={}, flushIntervalMs={}", 
                    maxBufferSize, flushIntervalMs);
    }

    @PreDestroy
    public void destroy() {
        logger.info("Flushing buffer before shutdown");
        flushBuffer();
    }

    /**
     * Add a log entry to the buffer.
     * If the buffer reaches the maximum size, it will be flushed automatically.
     *
     * @param logEntry The log entry to add
     * @return true if the entry was added, false otherwise
     */
    public boolean addToBuffer(LogEntry logEntry) {
        buffer.add(logEntry);
        int currentSize = bufferSize.incrementAndGet();
        
        logger.trace("Added log entry to buffer. Current buffer size: {}", currentSize);
        
        // If buffer is full, flush it
        if (currentSize >= maxBufferSize) {
            logger.info("Buffer reached maximum size ({}). Flushing...", maxBufferSize);
            flushBuffer();
        }
        
        return true;
    }

    /**
     * Add multiple log entries to the buffer.
     * The buffer will be flushed if it reaches the maximum size during this operation.
     *
     * @param logEntries The log entries to add
     * @return The number of entries added
     */
    public int addAllToBuffer(List<LogEntry> logEntries) {
        int addedCount = 0;
        
        for (LogEntry entry : logEntries) {
            if (addToBuffer(entry)) {
                addedCount++;
            }
        }
        
        logger.debug("Added {} log entries to buffer", addedCount);
        return addedCount;
    }

    /**
     * Flush the buffer, indexing all log entries currently in it.
     * This method is synchronized to prevent concurrent flushes.
     *
     * @return The number of entries flushed
     */
    public int flushBuffer() {
        // Use a lock to prevent concurrent flushes
        if (!flushLock.tryLock()) {
            logger.debug("Buffer flush already in progress, skipping");
            return 0;
        }
        
        try {
            int currentSize = bufferSize.get();
            if (currentSize == 0) {
                logger.debug("Buffer is empty, nothing to flush");
                return 0;
            }
            
            logger.info("Flushing buffer with {} log entries", currentSize);
            
            // Drain the buffer into a list
            List<LogEntry> entriesToFlush = new ArrayList<>(currentSize);
            LogEntry entry;
            while ((entry = buffer.poll()) != null) {
                entriesToFlush.add(entry);
            }
            
            // Reset buffer size
            bufferSize.set(0);
            
            // Index the entries
            try {
                int indexedCount = luceneService.indexLogEntries(entriesToFlush);
                logger.info("Successfully indexed {} log entries", indexedCount);
                return indexedCount;
            } catch (IOException e) {
                logger.error("Error indexing log entries during buffer flush", e);
                // In case of error, we could consider re-adding entries to the buffer
                // but that might cause memory issues if the error persists
                return 0;
            }
        } finally {
            flushLock.unlock();
        }
    }

    /**
     * Scheduled task to flush the buffer periodically.
     * This ensures that log entries are indexed even if the buffer doesn't fill up.
     */
    @Scheduled(fixedDelayString = "${grepwise.buffer.flush-interval-ms:30000}")
    public void scheduledFlush() {
        int currentSize = bufferSize.get();
        if (currentSize > 0) {
            logger.info("Performing scheduled buffer flush with {} entries", currentSize);
            flushBuffer();
        } else {
            logger.debug("Scheduled flush: buffer is empty, nothing to flush");
        }
    }

    /**
     * Get the current buffer size.
     *
     * @return The number of entries currently in the buffer
     */
    public int getBufferSize() {
        return bufferSize.get();
    }

    /**
     * Get the maximum buffer size.
     *
     * @return The maximum number of entries the buffer can hold before being flushed
     */
    public int getMaxBufferSize() {
        return maxBufferSize;
    }

    /**
     * Set the maximum buffer size.
     *
     * @param maxBufferSize The maximum number of entries the buffer can hold before being flushed
     */
    public void setMaxBufferSize(int maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
        logger.info("Maximum buffer size updated to {}", maxBufferSize);
    }

    /**
     * Get the flush interval in milliseconds.
     *
     * @return The interval at which the buffer is automatically flushed
     */
    public int getFlushIntervalMs() {
        return flushIntervalMs;
    }

    /**
     * Set the flush interval in milliseconds.
     *
     * @param flushIntervalMs The interval at which the buffer is automatically flushed
     */
    public void setFlushIntervalMs(int flushIntervalMs) {
        this.flushIntervalMs = flushIntervalMs;
        logger.info("Flush interval updated to {} ms", flushIntervalMs);
    }
}