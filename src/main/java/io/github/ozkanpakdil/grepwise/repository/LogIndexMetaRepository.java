package io.github.ozkanpakdil.grepwise.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ozkanpakdil.grepwise.GrepWiseApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists minimal indexing metadata for each scanned log file.
 * Stores last known file size and lastModified to avoid re-indexing unchanged files.
 */
@Repository
public class LogIndexMetaRepository {
    private static final Logger logger = LoggerFactory.getLogger(LogIndexMetaRepository.class);

    private static final String META_DIR = System.getProperty("user.home")
            + File.separator + "." + GrepWiseApplication.appName + File.separator + "index";
    private static final String META_FILE = META_DIR + File.separator + "log-index-meta.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, FileMeta> meta = new ConcurrentHashMap<>();

    public LogIndexMetaRepository() {
        load();
    }

    public synchronized void upsert(String absolutePath, long size, long lastModified) {
        meta.put(absolutePath, new FileMeta(size, lastModified));
        persist();
    }

    public FileMeta get(String absolutePath) {
        return meta.get(absolutePath);
    }

    /**
     * Returns true if there is no previous record for this file or if the stored size
     * differs from the provided currentSize. False means size is unchanged.
     */
    public boolean hasSizeChanged(String absolutePath, long currentSize) {
        FileMeta fm = meta.get(absolutePath);
        return fm == null || fm.size != currentSize;
    }

    public synchronized void remove(String absolutePath) {
        if (meta.remove(absolutePath) != null) {
            persist();
        }
    }

    private void load() {
        try {
            File file = new File(META_FILE);
            if (!file.exists()) {
                return;
            }
            Map<String, FileMeta> data = objectMapper.readValue(file, new TypeReference<>() {
            });
            meta.clear();
            meta.putAll(data);
            logger.info("Loaded {} log index meta entries", meta.size());
        } catch (Exception e) {
            logger.warn("Failed to load log index meta; starting fresh", e);
        }
    }

    private void persist() {
        try {
            File dir = new File(META_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                logger.warn("Could not create index meta directory at {}", dir.getAbsolutePath());
            }
            File file = new File(META_FILE);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, meta);
            logger.info("Persisted {} log index meta entries to {}", meta.size(), file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to persist log index meta", e);
        }
    }

    public static class FileMeta {
        public long size;
        public long lastModified;

        public FileMeta() {
        }

        public FileMeta(long size, long lastModified) {
            this.size = size;
            this.lastModified = lastModified;
        }
    }
}
