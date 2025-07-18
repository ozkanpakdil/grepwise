package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.ArchiveMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for storing and retrieving archive metadata.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class ArchiveMetadataRepository {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveMetadataRepository.class);
    private final Map<String, ArchiveMetadata> archives = new ConcurrentHashMap<>();

    /**
     * Constructor that initializes the repository.
     */
    public ArchiveMetadataRepository() {
        logger.info("Initializing ArchiveMetadataRepository");
    }

    /**
     * Save archive metadata.
     *
     * @param metadata The metadata to save
     * @return The saved metadata with a generated ID
     */
    public ArchiveMetadata save(ArchiveMetadata metadata) {
        if (metadata.getId() == null || metadata.getId().isEmpty()) {
            metadata.setId(UUID.randomUUID().toString());
        }
        archives.put(metadata.getId(), metadata);
        logger.debug("Saved archive metadata: {}", metadata);
        return metadata;
    }

    /**
     * Find archive metadata by ID.
     *
     * @param id The ID of the metadata to find
     * @return The metadata, or null if not found
     */
    public ArchiveMetadata findById(String id) {
        return archives.get(id);
    }

    /**
     * Find all archive metadata.
     *
     * @return A list of all metadata
     */
    public List<ArchiveMetadata> findAll() {
        return new ArrayList<>(archives.values());
    }

    /**
     * Find all available archive metadata sorted by creation time (newest first).
     *
     * @return A list of all available metadata sorted by creation time
     */
    public List<ArchiveMetadata> findAllAvailableSortedByCreationTime() {
        return archives.values().stream()
                .filter(ArchiveMetadata::isAvailable)
                .sorted(Comparator.comparing(ArchiveMetadata::getCreationTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Find archive metadata by source.
     *
     * @param source The source to filter by
     * @return A list of metadata for the specified source
     */
    public List<ArchiveMetadata> findBySource(String source) {
        return archives.values().stream()
                .filter(metadata -> metadata.getSources().contains(source))
                .collect(Collectors.toList());
    }

    /**
     * Find archive metadata by time range.
     *
     * @param startTimestamp The start timestamp
     * @param endTimestamp The end timestamp
     * @return A list of metadata that overlap with the specified time range
     */
    public List<ArchiveMetadata> findByTimeRange(long startTimestamp, long endTimestamp) {
        return archives.values().stream()
                .filter(metadata -> 
                    (metadata.getStartTimestamp() <= endTimestamp && 
                     metadata.getEndTimestamp() >= startTimestamp))
                .collect(Collectors.toList());
    }

    /**
     * Find archive metadata older than the specified timestamp.
     *
     * @param timestamp Archives created before this timestamp will be returned
     * @return A list of metadata created before the specified timestamp
     */
    public List<ArchiveMetadata> findOlderThan(Instant timestamp) {
        return archives.values().stream()
                .filter(metadata -> metadata.getCreationTime().isBefore(timestamp))
                .collect(Collectors.toList());
    }

    /**
     * Delete archive metadata by ID.
     *
     * @param id The ID of the metadata to delete
     * @return true if the metadata was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        ArchiveMetadata removed = archives.remove(id);
        if (removed != null) {
            logger.debug("Deleted archive metadata with ID: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Mark archive metadata as unavailable by ID.
     *
     * @param id The ID of the metadata to mark as unavailable
     * @return true if the metadata was marked as unavailable, false otherwise
     */
    public boolean markUnavailable(String id) {
        ArchiveMetadata metadata = archives.get(id);
        if (metadata != null) {
            metadata.setAvailable(false);
            archives.put(id, metadata);
            logger.debug("Marked archive metadata as unavailable: {}", id);
            return true;
        }
        return false;
    }

    /**
     * Delete all archive metadata.
     *
     * @return The number of metadata deleted
     */
    public int deleteAll() {
        int count = archives.size();
        archives.clear();
        logger.debug("Deleted all archive metadata, count: {}", count);
        return count;
    }

    /**
     * Get the total number of archive metadata.
     *
     * @return The total number of metadata
     */
    public int count() {
        return archives.size();
    }
}