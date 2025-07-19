package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.FieldConfiguration;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.service.ArchiveService;
import io.github.ozkanpakdil.grepwise.service.FieldConfigurationService;
import io.github.ozkanpakdil.grepwise.service.SearchCacheService;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for indexing and searching logs using Lucene.
 */
@Service
public class LuceneService {
    private static final Logger logger = LoggerFactory.getLogger(LuceneService.class);

    @Value("${grepwise.lucene.index-dir:./lucene-index}")
    private String indexDirPath;

    private Directory indexDirectory;
    private IndexWriter indexWriter;
    
    @Autowired
    private FieldConfigurationService fieldConfigurationService;
    
    @Autowired
    private ArchiveService archiveService;
    
    @Autowired
    private SearchCacheService searchCacheService;

    /**
     * Initialize the Lucene index.
     */
    @PostConstruct
    public void init() throws IOException {
        logger.info("Initializing Lucene index at {}", indexDirPath);

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

        logger.info("Lucene index initialized successfully");
    }

    /**
     * Close the Lucene index when the application shuts down.
     */
    @PreDestroy
    public void close() {
        try {
            if (indexWriter != null) {
                indexWriter.close();
            }
            if (indexDirectory != null) {
                indexDirectory.close();
            }
            logger.info("Lucene index closed successfully");
        } catch (IOException e) {
            logger.error("Error closing Lucene index", e);
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

        // Store metadata as StringFields
        if (logEntry.metadata() != null) {
            for (Map.Entry<String, String> entry : logEntry.metadata().entrySet()) {
                if (entry.getValue() != null) {
                    doc.add(new StringField("metadata_" + entry.getKey(), entry.getValue(), Field.Store.YES));
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
     */
    public int indexLogEntries(List<LogEntry> logEntries) throws IOException {
        for (LogEntry logEntry : logEntries) {
            Document doc = logEntryToDocument(logEntry);
            indexWriter.addDocument(doc);
        }
        indexWriter.commit();
        return logEntries.size();
    }

    /**
     * Search log entries by query string.
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
            Query textQuery;
            if (isRegex) {
                // Use RegexpQuery for regex searches
                textQuery = new RegexpQuery(new Term("message", queryStr));
            } else {
                // Use WildcardQuery for more flexible matching
                textQuery = new WildcardQuery(new Term("message", "*" + queryStr.toLowerCase() + "*"));
            }
            queryBuilder.add(textQuery, BooleanClause.Occur.MUST);
        }

        // Add time range query if provided
        if (startTime != null && endTime != null) {
            Query timeQuery = LongPoint.newRangeQuery("timestamp", startTime, endTime);
            queryBuilder.add(timeQuery, BooleanClause.Occur.MUST);
        }

        // Execute search
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(queryBuilder.build(), 1000); // Limit to 1000 results

            // Convert results to LogEntry objects
            List<LogEntry> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                results.add(documentToLogEntry(doc));
            }
            
            // Add results to cache
            searchCacheService.addToCache(queryStr, isRegex, startTime, endTime, results);
            logger.debug("Added search results to cache, result count: {}", results.size());

            return results;
        }
    }

    /**
     * Search log entries by level.
     */
    public List<LogEntry> findByLevel(String level) throws IOException {
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = new TermQuery(new Term("level", level));
            TopDocs topDocs = searcher.search(query, 1000);

            List<LogEntry> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                results.add(documentToLogEntry(doc));
            }

            return results;
        }
    }

    /**
     * Search log entries by source.
     */
    public List<LogEntry> findBySource(String source) throws IOException {
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = new TermQuery(new Term("source", source));
            TopDocs topDocs = searcher.search(query, 1000);

            List<LogEntry> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                results.add(documentToLogEntry(doc));
            }

            return results;
        }
    }

    /**
     * Get a log entry by ID.
     */
    public LogEntry findById(String id) throws IOException {
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            Query query = new TermQuery(new Term("id", id));
            TopDocs topDocs = searcher.search(query, 1);

            if (topDocs.scoreDocs.length > 0) {
                Document doc = searcher.storedFields().document(topDocs.scoreDocs[0].doc);
                return documentToLogEntry(doc);
            }

            return null;
        }
    }

    /**
     * Find logs matching a query.
     * 
     * @param query The query to match logs against
     * @return A list of matching logs
     */
    private List<LogEntry> findLogsByQuery(Query query) throws IOException {
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            // Use a higher limit to retrieve more logs for archiving
            TopDocs topDocs = searcher.search(query, 10000);
            
            List<LogEntry> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.storedFields().document(scoreDoc.doc);
                results.add(documentToLogEntry(doc));
            }
            
            return results;
        }
    }
    
    /**
     * Delete logs older than the specified timestamp.
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
        
        // Delete matching documents
        long deleted = indexWriter.deleteDocuments(query);
        indexWriter.commit();
        
        logger.info("Deleted {} logs older than timestamp: {}", deleted, timestamp);
        return deleted;
    }

    /**
     * Delete logs older than the specified timestamp for a specific source.
     * 
     * @param timestamp Logs older than this timestamp will be deleted
     * @param source The source to filter by
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
        
        // Delete matching documents
        long deleted = indexWriter.deleteDocuments(query);
        indexWriter.commit();
        
        logger.info("Deleted {} logs older than timestamp: {} for source: {}", deleted, timestamp, source);
        return deleted;
    }
}
