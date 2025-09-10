package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.repository.RedactionConfigRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Bridges the RedactionConfigRepository (file-backed) with RedactionUtil runtime patterns.
 */
@Service
public class RedactionConfigService {
    private static final Logger logger = LoggerFactory.getLogger(RedactionConfigService.class);

    private final RedactionConfigRepository repo;

    public RedactionConfigService(RedactionConfigRepository repo) {
        this.repo = repo;
    }

    @PostConstruct
    public void init() {
        Set<String> keys = repo.getKeys();
        List<String> patterns = repo.getPatterns();
        RedactionUtil.refresh(keys, patterns);
        logger.info("Initialized RedactionUtil with keys: {}, patterns: {}", keys, patterns);
    }

    public void reload() {
        repo.reload();
        Set<String> keys = repo.getKeys();
        List<String> patterns = repo.getPatterns();
        RedactionUtil.refresh(keys, patterns);
        logger.info("Reloaded RedactionUtil with keys: {}, patterns: {}", keys, patterns);
    }
}
