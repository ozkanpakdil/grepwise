package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service for caching search results to improve search performance.
 * This reduces the need to perform repeated identical searches against the Lucene index.
 */
@Service
public class SearchCacheService {
    private static final Logger logger = LoggerFactory.getLogger(SearchCacheService.class);

    // Cache structure: key = search parameters hash, value = CacheEntry
    private final Map<String, CacheEntry> cache;
    
    // Cache statistics
    private final AtomicInteger cacheSize;
    private final AtomicLong cacheHits;
    private final AtomicLong cacheMisses;
    private final AtomicLong cacheEvictions;

    @Value("${grepwise.search-cache.max-size:100}")
    private int maxCacheSize;

    @Value("${grepwise.search-cache.expiration-ms:300000}")
    private int expirationMs; // Default: 5 minutes

    @Value("${grepwise.search-cache.enabled:true}")
    private boolean cacheEnabled;

    /**
     * Inner class to store cache entries with their metadata
     */
    private static class CacheEntry {
        private final List<LogEntry> results;
        private final long creationTime;
        private long lastAccessTime;
        private final AtomicInteger accessCount;

        public CacheEntry(List<LogEntry> results) {
            this.results = results;
            this.creationTime = System.currentTimeMillis();
            this.lastAccessTime = this.creationTime;
            this.accessCount = new AtomicInteger(0);
        }

        public List<LogEntry> getResults() {
            this.lastAccessTime = System.currentTimeMillis();
            this.accessCount.incrementAndGet();
            return this.results;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public int getAccessCount() {
            return accessCount.get();
        }

        public boolean isExpired(long expirationMs) {
            return System.currentTimeMillis() - lastAccessTime > expirationMs;
        }
    }

    public SearchCacheService() {
        this.cache = new ConcurrentHashMap<>();
        this.cacheSize = new AtomicInteger(0);
        this.cacheHits = new AtomicLong(0);
        this.cacheMisses = new AtomicLong(0);
        this.cacheEvictions = new AtomicLong(0);
    }

    @PostConstruct
    public void init() {
        logger.info("SearchCacheService initialized with maxCacheSize={}, expirationMs={}, enabled={}",
                maxCacheSize, expirationMs, cacheEnabled);
    }

    /**
     * Generate a cache key from search parameters
     */
    private String generateCacheKey(String queryStr, boolean isRegex, Long startTime, Long endTime) {
        return String.format("%s:%b:%d:%d", 
                queryStr != null ? queryStr : "", 
                isRegex, 
                startTime != null ? startTime : 0, 
                endTime != null ? endTime : 0);
    }

    /**
     * Get search results from cache if available
     *
     * @param queryStr The search query string
     * @param isRegex Whether the query is a regex
     * @param startTime The start time for time-based filtering
     * @param endTime The end time for time-based filtering
     * @return The cached search results, or null if not in cache
     */
    public List<LogEntry> getFromCache(String queryStr, boolean isRegex, Long startTime, Long endTime) {
        if (!cacheEnabled) {
            return null;
        }

        String cacheKey = generateCacheKey(queryStr, isRegex, startTime, endTime);
        CacheEntry entry = cache.get(cacheKey);

        if (entry != null) {
            if (entry.isExpired(expirationMs)) {
                // Remove expired entry
                cache.remove(cacheKey);
                cacheSize.decrementAndGet();
                cacheEvictions.incrementAndGet();
                logger.debug("Cache entry expired for key: {}", cacheKey);
                cacheMisses.incrementAndGet();
                return null;
            }
            
            // Cache hit
            cacheHits.incrementAndGet();
            logger.debug("Cache hit for key: {}", cacheKey);
            return entry.getResults();
        }

        // Cache miss
        cacheMisses.incrementAndGet();
        logger.debug("Cache miss for key: {}", cacheKey);
        return null;
    }

    /**
     * Add search results to the cache
     *
     * @param queryStr The search query string
     * @param isRegex Whether the query is a regex
     * @param startTime The start time for time-based filtering
     * @param endTime The end time for time-based filtering
     * @param results The search results to cache
     */
    public void addToCache(String queryStr, boolean isRegex, Long startTime, Long endTime, List<LogEntry> results) {
        if (!cacheEnabled || results == null) {
            return;
        }

        String cacheKey = generateCacheKey(queryStr, isRegex, startTime, endTime);
        
        // Check if we need to evict entries to make room
        if (cacheSize.get() >= maxCacheSize) {
            evictOldestEntry();
        }
        
        cache.put(cacheKey, new CacheEntry(results));
        cacheSize.incrementAndGet();
        logger.debug("Added entry to cache for key: {}", cacheKey);
    }

    /**
     * Evict the oldest entry from the cache based on last access time
     */
    private void evictOldestEntry() {
        if (cache.isEmpty()) {
            return;
        }

        String oldestKey = null;
        long oldestAccessTime = Long.MAX_VALUE;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().getLastAccessTime() < oldestAccessTime) {
                oldestAccessTime = entry.getValue().getLastAccessTime();
                oldestKey = entry.getKey();
            }
        }

        if (oldestKey != null) {
            cache.remove(oldestKey);
            cacheSize.decrementAndGet();
            cacheEvictions.incrementAndGet();
            logger.debug("Evicted oldest cache entry with key: {}", oldestKey);
        }
    }

    /**
     * Scheduled task to clean up expired cache entries
     */
    @Scheduled(fixedDelayString = "${grepwise.search-cache.cleanup-interval-ms:60000}")
    public void cleanupExpiredEntries() {
        if (!cacheEnabled || cache.isEmpty()) {
            return;
        }

        logger.debug("Running scheduled cache cleanup");
        int removedCount = 0;

        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().isExpired(expirationMs)) {
                cache.remove(entry.getKey());
                cacheSize.decrementAndGet();
                cacheEvictions.incrementAndGet();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            logger.info("Removed {} expired cache entries during cleanup", removedCount);
        } else {
            logger.debug("No expired cache entries found during cleanup");
        }
    }

    /**
     * Clear the entire cache
     */
    public void clearCache() {
        int previousSize = cacheSize.getAndSet(0);
        cache.clear();
        cacheEvictions.addAndGet(previousSize);
        logger.info("Cache cleared, removed {} entries", previousSize);
    }

    /**
     * Get the current cache size
     */
    public int getCacheSize() {
        return cacheSize.get();
    }

    /**
     * Get the maximum cache size
     */
    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    /**
     * Set the maximum cache size
     */
    public void setMaxCacheSize(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        logger.info("Maximum cache size updated to {}", maxCacheSize);
        
        // If the new size is smaller than the current cache size, evict entries
        while (cacheSize.get() > maxCacheSize) {
            evictOldestEntry();
        }
    }

    /**
     * Get the cache expiration time in milliseconds
     */
    public int getExpirationMs() {
        return expirationMs;
    }

    /**
     * Set the cache expiration time in milliseconds
     */
    public void setExpirationMs(int expirationMs) {
        this.expirationMs = expirationMs;
        logger.info("Cache expiration time updated to {} ms", expirationMs);
    }

    /**
     * Check if the cache is enabled
     */
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    /**
     * Enable or disable the cache
     */
    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
        logger.info("Search cache {} ", cacheEnabled ? "enabled" : "disabled");
        
        // Clear the cache if it's being disabled
        if (!cacheEnabled) {
            clearCache();
        }
    }

    /**
     * Get cache statistics
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("enabled", cacheEnabled);
        stats.put("size", cacheSize.get());
        stats.put("maxSize", maxCacheSize);
        stats.put("expirationMs", expirationMs);
        stats.put("hits", cacheHits.get());
        stats.put("misses", cacheMisses.get());
        stats.put("evictions", cacheEvictions.get());
        
        // Calculate hit ratio if there have been any cache accesses
        long totalAccesses = cacheHits.get() + cacheMisses.get();
        double hitRatio = totalAccesses > 0 ? (double) cacheHits.get() / totalAccesses : 0.0;
        stats.put("hitRatio", hitRatio);
        
        return stats;
    }
}