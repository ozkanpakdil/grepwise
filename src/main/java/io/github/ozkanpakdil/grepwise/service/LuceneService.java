package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
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
    private StandardAnalyzer analyzer;
    private IndexWriter indexWriter;

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
        analyzer = new StandardAnalyzer();
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
            if (fieldName.startsWith("metadata_")) {
                String key = fieldName.substring("metadata_".length());
                String value = doc.get(fieldName);
                metadata.put(key, value);
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

}
