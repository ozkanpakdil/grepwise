package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.FieldConfiguration;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.PartitionConfiguration;
import io.github.ozkanpakdil.grepwise.repository.PartitionConfigurationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for indexing and searching logs using Lucene.
 * Supports time-based partitioning of log data for improved performance and manageability.
 *
 * What partitioning brings:
 * - Smaller, faster indices: each time slice (daily/weekly/monthly) is an independent Lucene index, reducing segment counts per search and write amplification.
 * - Faster searches with time filters: queries restricted by time can touch only the relevant active partitions instead of a monolithic index.
 * - Cheaper retention/archival: old partitions can be closed and archived as whole directories without costly per-document deletes.
 * - Lower write contention: parallel writes are distributed across active partitions, improving throughput.
 * - Operational safety: corruption or maintenance affects only a single partition, not the entire dataset.
 *
 * Configuration is provided via PartitionConfigurationRepository -> PartitionConfiguration (type, base dir,
 * max active partitions, auto-archive, enabled flag). See init(), initializePartitions(), checkAndRotatePartitions(),
 * indexLogEntries(), and search() for the lifecycle and usage.
 */
@Service
public class LuceneService {
    private static final Logger logger = LoggerFactory.getLogger(LuceneService.class);
    // Map of partition name to IndexWriter
    private final Map<String, IndexWriter> partitionWriters = new ConcurrentHashMap<>();
    // Map of partition name to Directory
    private final Map<String, Directory> partitionDirectories = new ConcurrentHashMap<>();
    // Current active partitions (ordered from newest to oldest)
    private final List<String> activePartitions = new ArrayList<>();
    @Value("${grepwise.lucene.index-dir:./lucene-index}")
    private String indexDirPath;
    // Legacy single index support
    private Directory indexDirectory;
    private IndexWriter indexWriter;
    // Partitioning support
    @Autowired
    private PartitionConfigurationRepository partitionConfigurationRepository;
    // Flag to indicate if partitioning is enabled
    private boolean partitioningEnabled = false;
    @Autowired
    private FieldConfigurationService fieldConfigurationService;
    @Autowired
    private ArchiveService archiveService;
    @Autowired
    private SearchCacheService searchCacheService;
    @Autowired
    @org.springframework.context.annotation.Lazy
    private RealTimeUpdateService realTimeUpdateService;

    /**
     * Set the partition configuration repository.
     * This method is primarily for testing and benchmarking purposes.
     *
     * @param partitionConfigurationRepository The partition configuration repository
     */
    public void setPartitionConfigurationRepository(PartitionConfigurationRepository partitionConfigurationRepository) {
        this.partitionConfigurationRepository = partitionConfigurationRepository;
    }

    /**
     * Set the field configuration service.
     * This method is primarily for testing and benchmarking purposes.
     *
     * @param fieldConfigurationService The field configuration service
     */
    public void setFieldConfigurationService(FieldConfigurationService fieldConfigurationService) {
        this.fieldConfigurationService = fieldConfigurationService;
    }

    /**
     * Set the archive service.
     * This method is primarily for testing and benchmarking purposes.
     *
     * @param archiveService The archive service
     */
    public void setArchiveService(ArchiveService archiveService) {
        this.archiveService = archiveService;
    }

    /**
     * Set the search cache service.
     * This method is primarily for testing and benchmarking purposes.
     *
     * @param searchCacheService The search cache service
     */
    public void setSearchCacheService(SearchCacheService searchCacheService) {
        this.searchCacheService = searchCacheService;
    }

    /**
     * Set the index directory path.
     * This method is primarily for testing and benchmarking purposes.
     *
     * @param indexPath The path to the index directory
     */
    public void setIndexPath(String indexPath) {
        this.indexDirPath = indexPath;
    }

    /**
     * Set whether partitioning is enabled.
     * This method is primarily for testing and benchmarking purposes.
     *
     * @param partitioningEnabled Whether partitioning is enabled
     */
    public void setPartitioningEnabled(boolean partitioningEnabled) {
        this.partitioningEnabled = partitioningEnabled;
    }

    /**
     * Set the real-time update service.
     * This method is primarily for testing and benchmarking purposes.
     *
     * @param realTimeUpdateService The real-time update service
     */
    public void setRealTimeUpdateService(RealTimeUpdateService realTimeUpdateService) {
        this.realTimeUpdateService = realTimeUpdateService;
    }

    /**
     * Initialize the Lucene index.
     */
    @PostConstruct
    public void init() throws IOException {
        logger.info("Initializing Lucene service");

        // Check if partitioning is enabled
        try {
            PartitionConfiguration config = partitionConfigurationRepository.getDefaultConfiguration();
            partitioningEnabled = config.isPartitioningEnabled();

            if (partitioningEnabled) {
                logger.info("Partitioning is enabled, initializing partitioned indices");
                logger.info("Partition config: type={}, baseDir={}, maxActivePartitions={}, autoArchive={}",
                        config.getPartitionType(),
                        config.getPartitionBaseDirectory(),
                        config.getMaxActivePartitions(),
                        config.isAutoArchivePartitions());
                initializePartitions(config);
            } else {
                logger.info("Partitioning is disabled, initializing single index at {}", indexDirPath);
                initializeSingleIndex();
            }
        } catch (Exception e) {
            logger.warn("Error checking partition configuration, falling back to single index: {}", e.getMessage());
            initializeSingleIndex();
        }

        logger.info("Lucene service initialized successfully");
    }

    /**
     * Initialize a single Lucene index (legacy mode).
     */
    private void initializeSingleIndex() throws IOException {
        logger.info("Initializing single Lucene index at {}", indexDirPath);

        // Create index directory if it doesn't exist
        Path indexPath = Paths.get(indexDirPath);
        if (!Files.exists(indexPath)) {
            Files.createDirectories(indexPath);
        }

        // Initialize Lucene components
        indexDirectory = FSDirectory.open(indexPath);
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        indexWriter = new IndexWriter(indexDirectory, config);

        logger.info("Single Lucene index initialized successfully");
    }

    /**
     * Initialize partitioned indices.
     */
    private void initializePartitions(PartitionConfiguration config) throws IOException {
        // Create base partition directory if it doesn't exist
        Path partitionBasePath = Paths.get(config.getPartitionBaseDirectory());
        if (!Files.exists(partitionBasePath)) {
            Files.createDirectories(partitionBasePath);
            logger.info("Created partition base directory: {}", partitionBasePath);
        }

        // Initialize current partition
        String currentPartition = getCurrentPartitionName(config);
        initializePartition(currentPartition, config);

        // Add current partition to active partitions
        activePartitions.add(currentPartition);

        // Initialize previous partitions based on configuration
        initializePreviousPartitions(config);

        logger.info("Partitioned Lucene indices initialized with {} active partitions: {}",
                activePartitions.size(), activePartitions);
    }

    /**
     * Get the current partition name based on the configuration.
     */
    private String getCurrentPartitionName(PartitionConfiguration config) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter;

        switch (config.getPartitionType()) {
            case "DAILY":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return "partition_" + now.format(formatter);
            case "WEEKLY":
                // Use ISO week format (yyyy-Www)
                formatter = DateTimeFormatter.ofPattern("yyyy-'W'ww");
                return "partition_" + now.format(formatter);
            case "MONTHLY":
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                return "partition_" + now.format(formatter);
        }
    }

    /**
     * Initialize a partition.
     */
    private void initializePartition(String partitionName, PartitionConfiguration config) throws IOException {
        // Create partition directory if it doesn't exist
        Path partitionPath = Paths.get(config.getPartitionBaseDirectory(), partitionName);
        if (!Files.exists(partitionPath)) {
            Files.createDirectories(partitionPath);
            logger.info("Created partition directory: {}", partitionPath);
        }

        // Initialize Lucene components for this partition
        Directory directory = FSDirectory.open(partitionPath);
        StandardAnalyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        IndexWriter writer = new IndexWriter(directory, writerConfig);

        // Store the writer and directory
        partitionWriters.put(partitionName, writer);
        partitionDirectories.put(partitionName, directory);

        logger.info("Initialized partition: {}", partitionName);
    }

    /**
     * Initialize previous partitions based on configuration.
     */
    private void initializePreviousPartitions(PartitionConfiguration config) throws IOException {
        int maxActivePartitions = config.getMaxActivePartitions();

        // If only one partition is allowed, we're done
        if (maxActivePartitions <= 1) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<String> previousPartitions = new ArrayList<>();

        // Generate names for previous partitions
        for (int i = 1; i < maxActivePartitions; i++) {
            LocalDateTime partitionTime;

            switch (config.getPartitionType()) {
                case "DAILY":
                    partitionTime = now.minusDays(i);
                    break;
                case "WEEKLY":
                    partitionTime = now.minusWeeks(i);
                    break;
                case "MONTHLY":
                default:
                    partitionTime = now.minusMonths(i);
                    break;
            }

            String partitionName = getPartitionNameForDate(partitionTime, config.getPartitionType());
            previousPartitions.add(partitionName);
        }

        // Initialize each previous partition
        for (String partitionName : previousPartitions) {
            Path partitionPath = Paths.get(config.getPartitionBaseDirectory(), partitionName);

            // Only initialize if the partition directory exists
            if (Files.exists(partitionPath)) {
                initializePartition(partitionName, config);
                activePartitions.add(partitionName);
            }
        }
    }

    /**
     * Get the partition name for a specific date.
     */
    private String getPartitionNameForDate(LocalDateTime date, String partitionType) {
        DateTimeFormatter formatter;

        switch (partitionType) {
            case "DAILY":
                formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                return "partition_" + date.format(formatter);
            case "WEEKLY":
                formatter = DateTimeFormatter.ofPattern("yyyy-'W'ww");
                return "partition_" + date.format(formatter);
            case "MONTHLY":
            default:
                formatter = DateTimeFormatter.ofPattern("yyyy-MM");
                return "partition_" + date.format(formatter);
        }
    }

    /**
     * Get the partition name for a specific timestamp.
     */
    private String getPartitionNameForTimestamp(long timestamp, String partitionType) {
        LocalDateTime date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return getPartitionNameForDate(date, partitionType);
    }

    /**
     * Check if a new partition should be created and old ones archived.
     * This is the rotation mechanism that enforces maxActivePartitions:
     * - If the wall clock moves into a new time slice, a new partition is created and added to the front.
     * - If the number of active partitions exceeds the limit, the oldest is closed and optionally archived.
     * Benefits: fast retention, bounded search/ingest working set, and predictable resource usage.
     */
    private void checkAndRotatePartitions() throws IOException {
        if (!partitioningEnabled) {
            return;
        }

        PartitionConfiguration config = partitionConfigurationRepository.getDefaultConfiguration();
        String currentPartition = getCurrentPartitionName(config);

        // If current partition doesn't exist, create it
        if (!activePartitions.contains(currentPartition)) {
            initializePartition(currentPartition, config);
            activePartitions.add(0, currentPartition); // Add at the beginning

            // If we have more active partitions than allowed, archive the oldest
            if (activePartitions.size() > config.getMaxActivePartitions()) {
                String oldestPartition = activePartitions.remove(activePartitions.size() - 1);

                // Close the writer and directory for the oldest partition
                if (partitionWriters.containsKey(oldestPartition)) {
                    partitionWriters.get(oldestPartition).close();
                    partitionWriters.remove(oldestPartition);
                }

                if (partitionDirectories.containsKey(oldestPartition)) {
                    partitionDirectories.get(oldestPartition).close();
                    partitionDirectories.remove(oldestPartition);
                }

                // Archive the oldest partition if auto-archiving is enabled
                if (config.isAutoArchivePartitions()) {
                    archivePartition(oldestPartition, config);
                }

                logger.info("Rotated partition: removed {} from active set", oldestPartition);
            }

            logger.info("Created new partition: {}, active partitions: {}", currentPartition, activePartitions);
        }
    }

    /**
     * Archive a partition.
     */
    private void archivePartition(String partitionName, PartitionConfiguration config) {
        // TODO: Implement partition archiving
        // This would involve:
        // 1. Reading all logs from the partition
        // 2. Using the ArchiveService to archive them
        // 3. Optionally deleting the partition directory

        logger.info("Archiving partition: {} (not implemented yet)", partitionName);
    }

    /**
     * Close the Lucene indices when the application shuts down.
     */
    @PreDestroy
    public void close() {
        try {
            // Close single index if it exists
            if (indexWriter != null) {
                indexWriter.close();
            }
            if (indexDirectory != null) {
                indexDirectory.close();
            }

            // Close all partition indices
            if (partitioningEnabled) {
                for (Map.Entry<String, IndexWriter> entry : partitionWriters.entrySet()) {
                    if (entry.getValue() != null) {
                        entry.getValue().close();
                    }
                }

                for (Map.Entry<String, Directory> entry : partitionDirectories.entrySet()) {
                    if (entry.getValue() != null) {
                        entry.getValue().close();
                    }
                }

                logger.info("All partition indices closed successfully");
            }

            logger.info("Lucene service closed successfully");
        } catch (IOException e) {
            logger.error("Error closing Lucene indices", e);
        }
    }

    /**
     * Convert a LogEntry to a Lucene Document.
     */
    private Document logEntryToDocument(LogEntry logEntry) {
        Document doc = new Document();

        // Store ID as a StringField (exact match, stored)
        doc.add(new StringField("id", logEntry.id(), Field.Store.YES));

        // Store timestamp as a LongPoint (for range queries) and StoredField (for retrieval)
        doc.add(new LongPoint("timestamp", logEntry.timestamp()));
        doc.add(new StoredField("timestamp_stored", logEntry.timestamp()));

        // Store recordTime if available
        if (logEntry.recordTime() != null) {
            doc.add(new LongPoint("recordTime", logEntry.recordTime()));
            doc.add(new StoredField("recordTime_stored", logEntry.recordTime()));
        }

        // Store level as a StringField (exact match, stored)
        if (logEntry.level() != null) {
            doc.add(new StringField("level", logEntry.level(), Field.Store.YES));
        }

        // Store message as a TextField (tokenized, stored)
        if (logEntry.message() != null) {
            doc.add(new TextField("message", logEntry.message(), Field.Store.YES));
        }

        // Store source as a StringField (exact match, stored)
        if (logEntry.source() != null) {
            doc.add(new StringField("source", logEntry.source(), Field.Store.YES));
        }

        // Store raw content as a TextField (tokenized, stored)
        if (logEntry.rawContent() != null) {
            doc.add(new TextField("rawContent", logEntry.rawContent(), Field.Store.YES));
        }

        // Store metadata as StringFields and TextFields for searchability
        if (logEntry.metadata() != null) {
            for (Map.Entry<String, String> entry : logEntry.metadata().entrySet()) {
                if (entry.getValue() != null) {
                    String fieldName = "metadata_" + entry.getKey();

                    // Store as StringField for exact matching and retrieval
                    doc.add(new StringField(fieldName, entry.getValue(), Field.Store.YES));

                    // Important fields that need to be searchable by content
                    // Add as TextField for tokenized searching
                    if (entry.getKey().equals("ip_address") ||
                            entry.getKey().equals("client_ip") ||
                            entry.getKey().equals("path") ||
                            entry.getKey().equals("request")) {
                        doc.add(new TextField(fieldName + "_text", entry.getValue(), Field.Store.NO));
                    }
                }
            }
        }

        // Apply field configurations to extract and index custom fields
        try {
            List<FieldConfiguration> enabledConfigs = fieldConfigurationService.getAllEnabledFieldConfigurations();
            for (FieldConfiguration config : enabledConfigs) {
                // Get the source field value based on the configuration
                String sourceValue = null;
                switch (config.getSourceField()) {
                    case "message":
                        sourceValue = logEntry.message();
                        break;
                    case "level":
                        sourceValue = logEntry.level();
                        break;
                    case "source":
                        sourceValue = logEntry.source();
                        break;
                    case "rawContent":
                        sourceValue = logEntry.rawContent();
                        break;
                    default:
                        // Check if it's a metadata field
                        if (config.getSourceField().startsWith("metadata_") && logEntry.metadata() != null) {
                            String metadataKey = config.getSourceField().substring("metadata_".length());
                            sourceValue = logEntry.metadata().get(metadataKey);
                        }
                        break;
                }

                // Skip if source value is null or empty
                if (sourceValue == null || sourceValue.isEmpty()) {
                    continue;
                }

                // Extract the field value using the configuration
                String fieldValue = fieldConfigurationService.extractFieldValue(config, sourceValue);
                if (fieldValue == null || fieldValue.isEmpty()) {
                    continue;
                }

                // Add the field to the document based on its type and configuration
                String fieldName = "custom_" + config.getName();

                switch (config.getFieldType()) {
                    case STRING:
                        if (config.isTokenized()) {
                            doc.add(new TextField(fieldName, fieldValue, config.isStored() ? Field.Store.YES : Field.Store.NO));
                        } else {
                            doc.add(new StringField(fieldName, fieldValue, config.isStored() ? Field.Store.YES : Field.Store.NO));
                        }
                        break;
                    case NUMBER:
                        try {
                            long numValue = Long.parseLong(fieldValue);
                            if (config.isIndexed()) {
                                doc.add(new LongPoint(fieldName, numValue));
                                doc.add(new NumericDocValuesField(fieldName + "_sort", numValue));
                            }
                            if (config.isStored()) {
                                doc.add(new StoredField(fieldName + "_stored", numValue));
                            }
                        } catch (NumberFormatException e) {
                            logger.warn("Failed to parse number field value: {}", fieldValue, e);
                        }
                        break;
                    case DATE:
                        try {
                            long dateValue = Long.parseLong(fieldValue);
                            if (config.isIndexed()) {
                                doc.add(new LongPoint(fieldName, dateValue));
                                doc.add(new NumericDocValuesField(fieldName + "_sort", dateValue));
                            }
                            if (config.isStored()) {
                                doc.add(new StoredField(fieldName + "_stored", dateValue));
                            }
                        } catch (NumberFormatException e) {
                            logger.warn("Failed to parse date field value: {}", fieldValue, e);
                        }
                        break;
                    case BOOLEAN:
                        boolean boolValue = Boolean.parseBoolean(fieldValue);
                        doc.add(new StringField(fieldName, Boolean.toString(boolValue), config.isStored() ? Field.Store.YES : Field.Store.NO));
                        break;
                }
            }
        } catch (Exception e) {
            logger.error("Error applying field configurations", e);
        }

        return doc;
    }

    /**
     * Convert a Lucene Document back to a LogEntry.
     */
    private LogEntry documentToLogEntry(Document doc) {
        String id = doc.get("id");
        long timestamp = Long.parseLong(doc.get("timestamp_stored"));

        Long recordTime = null;
        if (doc.get("recordTime_stored") != null) {
            recordTime = Long.parseLong(doc.get("recordTime_stored"));
        }

        String level = doc.get("level");
        String message = doc.get("message");
        String source = doc.get("source");
        String rawContent = doc.get("rawContent");

        // Extract metadata
        Map<String, String> metadata = new HashMap<>();
        for (IndexableField field : doc.getFields()) {
            String fieldName = field.name();

            // Handle standard metadata fields
            if (fieldName.startsWith("metadata_")) {
                String key = fieldName.substring("metadata_".length());
                String value = doc.get(fieldName);
                metadata.put(key, value);
            }

            // Handle custom fields
            else if (fieldName.startsWith("custom_")) {
                String key = fieldName.substring("custom_".length());

                // For numeric and date fields, we need to get the stored value
                if (doc.get(fieldName + "_stored") != null) {
                    String value = doc.get(fieldName + "_stored");
                    metadata.put(key, value);
                }
                // For string and boolean fields, we can get the value directly
                else if (doc.get(fieldName) != null) {
                    String value = doc.get(fieldName);
                    metadata.put(key, value);
                }
            }
        }

        return new LogEntry(id, timestamp, recordTime, level, message, source, metadata, rawContent);
    }

    /**
     * Index multiple log entries.
     * If partitioning is enabled, routes log entries to the appropriate partition based on their timestamp.
     * Otherwise, uses the legacy single index approach.
     */
    public int indexLogEntries(List<LogEntry> logEntries) throws IOException {
        if (logEntries == null || logEntries.isEmpty()) {
            return 0;
        }

        if (!partitioningEnabled) {
            // Legacy single index approach
            for (LogEntry logEntry : logEntries) {
                Document doc = logEntryToDocument(logEntry);
                indexWriter.addDocument(doc);

                // Broadcast log update for real-time notifications
                if (realTimeUpdateService != null) {
                    try {
                        realTimeUpdateService.broadcastLogUpdate(logEntry);
                    } catch (Exception e) {
                        logger.warn("Failed to broadcast log update: {}", e.getMessage());
                    }
                }
            }
            indexWriter.commit();
            return logEntries.size();
        }

        // Partitioning approach

        // Check if partitions need to be rotated
        checkAndRotatePartitions();

        // Get the current configuration
        PartitionConfiguration config = partitionConfigurationRepository.getDefaultConfiguration();

        // Group log entries by partition
        Map<String, List<LogEntry>> entriesByPartition = new HashMap<>();

        for (LogEntry logEntry : logEntries) {
            String partitionName = getPartitionNameForTimestamp(logEntry.timestamp(), config.getPartitionType());

            // If this partition is not active, use the current partition
            if (!activePartitions.contains(partitionName)) {
                partitionName = activePartitions.get(0); // Current partition is always first
            }

            // Add the log entry to the appropriate partition group
            if (!entriesByPartition.containsKey(partitionName)) {
                entriesByPartition.put(partitionName, new ArrayList<>());
            }
            entriesByPartition.get(partitionName).add(logEntry);
        }

        // Index log entries in their respective partitions
        int totalIndexed = 0;

        for (Map.Entry<String, List<LogEntry>> entry : entriesByPartition.entrySet()) {
            String partitionName = entry.getKey();
            List<LogEntry> partitionEntries = entry.getValue();

            IndexWriter writer = partitionWriters.get(partitionName);
            if (writer != null) {
                for (LogEntry logEntry : partitionEntries) {
                    Document doc = logEntryToDocument(logEntry);
                    Term idTerm = createDocumentIdTerm(logEntry);
                    writer.updateDocument(idTerm, doc);

                    // Broadcast log update for real-time notifications
                    try {
                        realTimeUpdateService.broadcastLogUpdate(logEntry);
                    } catch (Exception e) {
                        logger.warn("Failed to broadcast log update: {}", e.getMessage());
                    }
                }
                writer.commit();
                totalIndexed += partitionEntries.size();

                logger.trace("Indexed {} log entries in partition {}", partitionEntries.size(), partitionName);
            } else {
                logger.warn("No writer found for partition: {}, using legacy index", partitionName);

                // Fall back to legacy index if partition writer not found
                for (LogEntry logEntry : partitionEntries) {
                    Document doc = logEntryToDocument(logEntry);
                    Term idTerm = createDocumentIdTerm(logEntry);
                    indexWriter.updateDocument(idTerm, doc);

                    // Broadcast log update for real-time notifications
                    try {
                        realTimeUpdateService.broadcastLogUpdate(logEntry);
                    } catch (Exception e) {
                        logger.warn("Failed to broadcast log update: {}", e.getMessage());
                    }
                }

                indexWriter.commit();
                totalIndexed += partitionEntries.size();
            }
        }

        return totalIndexed;
    }

    /**
     * Create a unique identifier term for a log entry document.
     * This term is used by updateDocument to identify existing documents for updates.
     *
     * @param logEntry The log entry to create an identifier term for
     * @return A Term that uniquely identifies the document
     */
    private Term createDocumentIdTerm(LogEntry logEntry) {
        return new Term("rawContent", logEntry.rawContent());
    }


    /**
     * Search log entries by query string.
     * If partitioning is enabled, searches across all active partitions.
     * Otherwise, uses the legacy single index approach.
     * <p>
     * Note: This method is used directly by the ShardManagerService for local searches.
     * External callers should use the ShardManagerService for distributed searches.
     */
    public List<LogEntry> search(String queryStr, boolean isRegex, Long startTime, Long endTime) throws IOException {
        // If no query and no time range, return empty list
        if ((queryStr == null || queryStr.isEmpty()) && startTime == null && endTime == null) {
            return new ArrayList<>();
        }

        // Check cache first
        List<LogEntry> cachedResults = searchCacheService.getFromCache(queryStr, isRegex, startTime, endTime);
        if (cachedResults != null) {
            logger.debug("Returning cached results for query: {}, isRegex: {}, timeRange: [{} - {}]",
                    queryStr, isRegex, startTime, endTime);
            return cachedResults;
        }

        logger.debug("Cache miss, performing search for query: {}, isRegex: {}, timeRange: [{} - {}]",
                queryStr, isRegex, startTime, endTime);

        // Create a boolean query
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();

        // Add text query if provided
        if (queryStr != null && !queryStr.isEmpty()) {
            BooleanQuery.Builder textQueryBuilder = new BooleanQuery.Builder();

            // Fields to search in for parser-based queries (support phrases and multi-term)
            // Restrict to visible fields by default to avoid matches that don't appear in Message column.
            String[] parserFields = {
                    "message", "rawContent"
            };

            // If regex, keep existing behavior over specific fields
            if (isRegex) {
                String[] regexFields = {"message", "rawContent", "metadata_ip_address", "metadata_client_ip", "metadata_path", "metadata_request",
                        "metadata_ip_address_text", "metadata_client_ip_text", "metadata_path_text", "metadata_request_text"};
                for (String field : regexFields) {
                    Query fieldQuery = new RegexpQuery(new Term(field, queryStr));
                    textQueryBuilder.add(fieldQuery, BooleanClause.Occur.SHOULD);
                }
                queryBuilder.add(textQueryBuilder.build(), BooleanClause.Occur.MUST);
            } else {
                // Non-regex: use MultiFieldQueryParser to support quoted phrases and analyzers
                String qs = queryStr.trim();
                // Some frontends append a caret for boosting; if present standalone, strip it to avoid parse errors
                if (qs.endsWith("^") && (qs.chars().filter(ch -> ch == '^').count() == 1)) {
                    qs = qs.substring(0, qs.length() - 1).trim();
                }
                StandardAnalyzer analyzer = new StandardAnalyzer();
                MultiFieldQueryParser parser = new MultiFieldQueryParser(parserFields, analyzer);
                // Require all terms to be present to avoid unrelated matches
                parser.setDefaultOperator(QueryParser.Operator.AND);
                try {
                    Query parsed = parser.parse(qs);
                    queryBuilder.add(parsed, BooleanClause.Occur.MUST);
                } catch (ParseException e) {
                    // Fallback to wildcard queries if parsing fails; restrict to visible fields
                    String lowered = qs.toLowerCase();
                    String[] fallbackFields1 = {"message", "rawContent"};
                    for (String field : fallbackFields1) {
                        Query fieldQuery = new WildcardQuery(new Term(field, "*" + lowered + "*"));
                        textQueryBuilder.add(fieldQuery, BooleanClause.Occur.SHOULD);
                    }
                    queryBuilder.add(textQueryBuilder.build(), BooleanClause.Occur.MUST);
                }
            }
        }

        // Add time range query if provided
        if (startTime != null && endTime != null) {
            // Create a boolean query for time range that checks both timestamp and recordTime
            BooleanQuery.Builder timeQueryBuilder = new BooleanQuery.Builder();

            // Add query for system timestamp (when the log was indexed)
            Query timestampQuery = LongPoint.newRangeQuery("timestamp", startTime, endTime);
            timeQueryBuilder.add(timestampQuery, BooleanClause.Occur.SHOULD);

            // Add query for record timestamp (from the log entry itself)
            // This is the actual time when the log was generated
            Query recordTimeQuery = LongPoint.newRangeQuery("recordTime", startTime, endTime);
            timeQueryBuilder.add(recordTimeQuery, BooleanClause.Occur.SHOULD);

            queryBuilder.add(timeQueryBuilder.build(), BooleanClause.Occur.MUST);
        }

        Query query = queryBuilder.build();
        List<LogEntry> results = new ArrayList<>();

        if (!partitioningEnabled) {
            // Legacy single index approach
            try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs topDocs = searcher.search(query, 1000); // Limit to 1000 results

                // Convert results to LogEntry objects
                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.storedFields().document(scoreDoc.doc);
                    results.add(documentToLogEntry(doc));
                }
            }
        } else {
            // Partitioning approach - search across all active partitions
            for (String partitionName : activePartitions) {
                Directory directory = partitionDirectories.get(partitionName);
                if (directory != null) {
                    try (IndexReader reader = DirectoryReader.open(directory)) {
                        IndexSearcher searcher = new IndexSearcher(reader);
                        TopDocs topDocs = searcher.search(query, 1000); // Limit to 1000 results per partition

                        // Convert results to LogEntry objects
                        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                            Document doc = searcher.storedFields().document(scoreDoc.doc);
                            results.add(documentToLogEntry(doc));
                        }

                        logger.debug("Found {} results in partition {}", topDocs.scoreDocs.length, partitionName);
                    } catch (IOException e) {
                        logger.error("Error searching partition: {}", partitionName, e);
                    }
                }
            }
        }

        // Add results to cache
        searchCacheService.addToCache(queryStr, isRegex, startTime, endTime, results);
        logger.debug("Added search results to cache, result count: {}", results.size());

        return results;
    }

    /**
     * Search log entries by level.
     * If partitioning is enabled, searches across all active partitions.
     * Otherwise, uses the legacy single index approach.
     */
    public List<LogEntry> findByLevel(String level) throws IOException {
        Query query = new TermQuery(new Term("level", level));
        List<LogEntry> results = new ArrayList<>();

        if (!partitioningEnabled) {
            // Legacy single index approach
            try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs topDocs = searcher.search(query, 1000);

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.storedFields().document(scoreDoc.doc);
                    results.add(documentToLogEntry(doc));
                }
            }
        } else {
            // Partitioning approach - search across all active partitions
            for (String partitionName : activePartitions) {
                Directory directory = partitionDirectories.get(partitionName);
                if (directory != null) {
                    try (IndexReader reader = DirectoryReader.open(directory)) {
                        IndexSearcher searcher = new IndexSearcher(reader);
                        TopDocs topDocs = searcher.search(query, 1000); // Limit to 1000 results per partition

                        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                            Document doc = searcher.storedFields().document(scoreDoc.doc);
                            results.add(documentToLogEntry(doc));
                        }

                        logger.debug("Found {} logs with level {} in partition {}",
                                topDocs.scoreDocs.length, level, partitionName);
                    } catch (IOException e) {
                        logger.error("Error searching partition: {}", partitionName, e);
                    }
                }
            }
        }

        return results;
    }

    /**
     * Search log entries by source.
     * If partitioning is enabled, searches across all active partitions.
     * Otherwise, uses the legacy single index approach.
     */
    public List<LogEntry> findBySource(String source) throws IOException {
        Query query = new TermQuery(new Term("source", source));
        List<LogEntry> results = new ArrayList<>();

        if (!partitioningEnabled) {
            // Legacy single index approach
            try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs topDocs = searcher.search(query, 1000);

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.storedFields().document(scoreDoc.doc);
                    results.add(documentToLogEntry(doc));
                }
            }
        } else {
            // Partitioning approach - search across all active partitions
            for (String partitionName : activePartitions) {
                Directory directory = partitionDirectories.get(partitionName);
                if (directory != null) {
                    try (IndexReader reader = DirectoryReader.open(directory)) {
                        IndexSearcher searcher = new IndexSearcher(reader);
                        TopDocs topDocs = searcher.search(query, 1000); // Limit to 1000 results per partition

                        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                            Document doc = searcher.storedFields().document(scoreDoc.doc);
                            results.add(documentToLogEntry(doc));
                        }

                        logger.debug("Found {} logs with source {} in partition {}",
                                topDocs.scoreDocs.length, source, partitionName);
                    } catch (IOException e) {
                        logger.error("Error searching partition: {}", partitionName, e);
                    }
                }
            }
        }

        return results;
    }

    /**
     * Get a log entry by ID.
     * If partitioning is enabled, searches across all active partitions.
     * Otherwise, uses the legacy single index approach.
     */
    public LogEntry findById(String id) throws IOException {
        Query query = new TermQuery(new Term("id", id));

        if (!partitioningEnabled) {
            // Legacy single index approach
            try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs topDocs = searcher.search(query, 1);

                if (topDocs.scoreDocs.length > 0) {
                    Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
                    return documentToLogEntry(doc);
                }
            }
        } else {
            // Partitioning approach - search across all active partitions
            for (String partitionName : activePartitions) {
                Directory directory = partitionDirectories.get(partitionName);
                if (directory != null) {
                    try (IndexReader reader = DirectoryReader.open(directory)) {
                        IndexSearcher searcher = new IndexSearcher(reader);
                        TopDocs topDocs = searcher.search(query, 1);

                        if (topDocs.scoreDocs.length > 0) {
                            Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
                            logger.debug("Found log with ID {} in partition {}", id, partitionName);
                            return documentToLogEntry(doc);
                        }
                    } catch (IOException e) {
                        logger.error("Error searching partition: {}", partitionName, e);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Find logs matching a query.
     * If partitioning is enabled, searches across all active partitions.
     * Otherwise, uses the legacy single index approach.
     *
     * @param query The query to match logs against
     * @return A list of matching logs
     */
    private List<LogEntry> findLogsByQuery(Query query) throws IOException {
        List<LogEntry> results = new ArrayList<>();

        if (!partitioningEnabled) {
            // Legacy single index approach
            try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                // Use a higher limit to retrieve more logs for archiving
                TopDocs topDocs = searcher.search(query, 10000);

                for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                    Document doc = searcher.storedFields().document(scoreDoc.doc);
                    results.add(documentToLogEntry(doc));
                }
            }
        } else {
            // Partitioning approach - search across all active partitions
            for (String partitionName : activePartitions) {
                Directory directory = partitionDirectories.get(partitionName);
                if (directory != null) {
                    try (IndexReader reader = DirectoryReader.open(directory)) {
                        IndexSearcher searcher = new IndexSearcher(reader);
                        // Use a higher limit to retrieve more logs for archiving
                        TopDocs topDocs = searcher.search(query, 10000);

                        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                            Document doc = searcher.storedFields().document(scoreDoc.doc);
                            results.add(documentToLogEntry(doc));
                        }

                        logger.debug("Found {} matching logs in partition {}",
                                topDocs.scoreDocs.length, partitionName);
                    } catch (IOException e) {
                        logger.error("Error searching partition: {}", partitionName, e);
                    }
                }
            }
        }

        return results;
    }

    /**
     * Delete logs older than the specified timestamp.
     * If partitioning is enabled, deletes from all active partitions.
     * Otherwise, uses the legacy single index approach.
     *
     * @param timestamp Logs older than this timestamp will be deleted
     * @return The number of logs deleted
     */
    public long deleteLogsOlderThan(long timestamp) throws IOException {
        logger.info("Deleting logs older than timestamp: {}", timestamp);

        // Create a query for logs older than the timestamp
        Query query = LongPoint.newRangeQuery("timestamp", 0, timestamp);

        // Find logs to archive before deletion
        List<LogEntry> logsToDelete = findLogsByQuery(query);
        logger.info("Found {} logs to delete older than timestamp: {}", logsToDelete.size(), timestamp);

        // Archive logs before deletion if there are any
        if (!logsToDelete.isEmpty()) {
            boolean archived = archiveService.archiveLogsBeforeDeletion(logsToDelete);
            if (!archived) {
                logger.warn("Failed to archive logs before deletion. Proceeding with deletion anyway.");
            } else {
                logger.info("Successfully archived {} logs before deletion", logsToDelete.size());
            }
        }

        long totalDeleted = 0;

        if (!partitioningEnabled) {
            // Legacy single index approach
            long deleted = indexWriter.deleteDocuments(query);
            indexWriter.commit();
            totalDeleted = deleted;
        } else {
            // Partitioning approach - delete from all active partitions
            for (String partitionName : activePartitions) {
                IndexWriter writer = partitionWriters.get(partitionName);
                if (writer != null) {
                    long deleted = writer.deleteDocuments(query);
                    writer.commit();
                    totalDeleted += deleted;

                    logger.debug("Deleted {} logs older than timestamp: {} in partition {}",
                            deleted, timestamp, partitionName);
                }
            }
        }

        logger.info("Deleted a total of {} logs older than timestamp: {}", totalDeleted, timestamp);
        return totalDeleted;
    }

    /**
     * Delete logs older than the specified timestamp for a specific source.
     * If partitioning is enabled, deletes from all active partitions.
     * Otherwise, uses the legacy single index approach.
     *
     * @param timestamp Logs older than this timestamp will be deleted
     * @param source    The source to filter by
     * @return The number of logs deleted
     */
    public long deleteLogsOlderThanForSource(long timestamp, String source) throws IOException {
        logger.info("Deleting logs older than timestamp: {} for source: {}", timestamp, source);

        // Create a boolean query combining timestamp and source
        BooleanQuery.Builder queryBuilder = new BooleanQuery.Builder();
        queryBuilder.add(LongPoint.newRangeQuery("timestamp", 0, timestamp), BooleanClause.Occur.MUST);
        queryBuilder.add(new TermQuery(new Term("source", source)), BooleanClause.Occur.MUST);
        Query query = queryBuilder.build();

        // Find logs to archive before deletion
        List<LogEntry> logsToDelete = findLogsByQuery(query);
        logger.info("Found {} logs to delete older than timestamp: {} for source: {}",
                logsToDelete.size(), timestamp, source);

        // Archive logs before deletion if there are any
        if (!logsToDelete.isEmpty()) {
            boolean archived = archiveService.archiveLogsBeforeDeletion(logsToDelete);
            if (!archived) {
                logger.warn("Failed to archive logs before deletion. Proceeding with deletion anyway.");
            } else {
                logger.info("Successfully archived {} logs before deletion", logsToDelete.size());
            }
        }

        long totalDeleted = 0;

        if (!partitioningEnabled) {
            // Legacy single index approach
            long deleted = indexWriter.deleteDocuments(query);
            indexWriter.commit();
            totalDeleted = deleted;
        } else {
            // Partitioning approach - delete from all active partitions
            for (String partitionName : activePartitions) {
                IndexWriter writer = partitionWriters.get(partitionName);
                if (writer != null) {
                    long deleted = writer.deleteDocuments(query);
                    writer.commit();
                    totalDeleted += deleted;

                    logger.debug("Deleted {} logs older than timestamp: {} for source: {} in partition {}",
                            deleted, timestamp, source, partitionName);
                }
            }
        }

        logger.info("Deleted a total of {} logs older than timestamp: {} for source: {}",
                totalDeleted, timestamp, source);
        return totalDeleted;
    }
}
