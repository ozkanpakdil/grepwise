package io.github.ozkanpakdil.grepwise.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

import static io.github.ozkanpakdil.grepwise.GrepWiseApplication.CONFIG_DIR;

@Repository
public class RedactionConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(RedactionConfigRepository.class);
    private static final String FILE_NAME = "redaction.json";

    // Default configuration constants
    private static final List<String> DEFAULT_KEYS = Arrays.asList("password", "passwd");
    private static final List<String> DEFAULT_PATTERNS = List.of(
            "(\\\"(?i:password|passwd)\\\"\\s*:\\s*)\\\"([^\\\\\\\"]*)\\\"",
            "((?i:password|passwd)\\s*[=:]\\s*)([^\\\\n\\\\r\\\\t ]+)",
            "((?i:password|passwd)\\s*:\\s*)([^\\\\n\\\\r]+)",
            "((?i:password|passwd)\\s*->\\s*)([^\\\\n\\\\r]+)",
            "((?i:password|passwd)\\b)(\\s+)([^\\\\n\\\\r]+)"
    );

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final File dataFile = new File(CONFIG_DIR + File.separator + FILE_NAME);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile Set<String> keys = new LinkedHashSet<>();
    private volatile List<String> patterns = new ArrayList<>();
    private volatile List<Group> groups = new ArrayList<>();

    public static class Group {
        public List<String> keys = new ArrayList<>();
        public List<String> patterns = new ArrayList<>();
    }

    @PostConstruct
    public void init() {
        ensureConfigDirectory();
        if (!dataFile.exists()) {
            initializeWithDefaults();
        } else {
            loadConfiguration();
        }
    }

    private void ensureConfigDirectory() {
        File dir = dataFile.getParentFile();
        if (dir != null && !dir.exists()) {
            if (dir.mkdirs()) {
                logger.info("Created config directory: {}", dir.getAbsolutePath());
            }
        }
    }

    private void initializeWithDefaults() {
        Group defaultGroup = createDefaultGroup();
        groups = new ArrayList<>(List.of(defaultGroup));
        recomputeFlatFromGroups();
        persistConfiguration();
        logger.info("Created default redaction config at {} with keys {}", dataFile.getAbsolutePath(), keys);
    }

    private void loadConfiguration() {
        try {
            JsonNode root = mapper.readTree(dataFile);
            List<Group> loadedGroups = parseGroupsFromJson(root);

            if (loadedGroups.isEmpty()) {
                loadedGroups.add(createDefaultGroup());
            }

            updateGroupsAndRecompute(loadedGroups);
            persistConfiguration(); // Ensure consistency

            logger.info("Loaded redaction groups: {} groups, flat keys: {}, patterns: {}",
                    groups.size(), keys, patterns);
        } catch (Exception e) {
            logger.warn("Failed to load redaction config, falling back to defaults: {}", e.getMessage());
            initializeWithDefaults();
        }
    }

    private List<Group> parseGroupsFromJson(JsonNode root) throws IOException {
        List<Group> loadedGroups = new ArrayList<>();

        if (root != null && root.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = root.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                Group group = parseGroupFromEntry(entry);
                loadedGroups.add(group);
            }
        }

        return loadedGroups;
    }

    private Group parseGroupFromEntry(Map.Entry<String, JsonNode> entry) throws IOException {
        Group group = new Group();
        String propertyName = entry.getKey();
        JsonNode value = entry.getValue();

        // Parse keys from property name
        group.keys = parseKeysFromPropertyName(propertyName);

        // Parse patterns from value
        group.patterns = parsePatternsFromValue(value);

        return group;
    }

    private List<String> parseKeysFromPropertyName(String propertyName) throws IOException {
        String trimmed = propertyName == null ? "" : propertyName.trim();

        if (trimmed.startsWith("[")) {
            // Handle JSON array format
            try {
                List<String> arrayKeys = mapper.readValue(trimmed, new TypeReference<List<String>>() {});
                return arrayKeys.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            } catch (Exception e) {
                return new ArrayList<>(List.of(propertyName));
            }
        } else if (!trimmed.isEmpty()) {
            return new ArrayList<>(List.of(propertyName));
        } else {
            return new ArrayList<>();
        }
    }

    private List<String> parsePatternsFromValue(JsonNode value) {
        List<String> patterns = new ArrayList<>();

        if (value != null && value.has("patterns") && value.get("patterns").isArray()) {
            for (JsonNode pattern : value.get("patterns")) {
                if (pattern != null && pattern.isTextual()) {
                    String patternText = pattern.asText().trim();
                    if (!patternText.isEmpty()) {
                        patterns.add(patternText);
                    }
                }
            }
        }

        return patterns;
    }

    private Group createDefaultGroup() {
        Group group = new Group();
        group.keys = new ArrayList<>(DEFAULT_KEYS);
        group.patterns = new ArrayList<>(DEFAULT_PATTERNS);
        return group;
    }

    private void updateGroupsAndRecompute(List<Group> newGroups) {
        lock.writeLock().lock();
        try {
            this.groups = newGroups;
            recomputeFlatFromGroups();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void recomputeFlatFromGroups() {
        LinkedHashSet<String> keySet = new LinkedHashSet<>();
        ArrayList<String> patternList = new ArrayList<>();

        for (Group group : groups) {
            if (group.keys != null) keySet.addAll(group.keys);
            if (group.patterns != null) patternList.addAll(group.patterns);
        }

        // Ensure defaults are always present
        keySet.addAll(DEFAULT_KEYS);

        this.keys = keySet;
        this.patterns = patternList;
    }

    private void persistConfiguration() {
        lock.readLock().lock();
        try {
            mapper.writeValue(dataFile, buildGroupedMap());
        } catch (IOException e) {
            logger.warn("Failed to persist redaction config: {}", e.getMessage());
        } finally {
            lock.readLock().unlock();
        }
    }

    private LinkedHashMap<String, Object> buildGroupedMap() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();

        for (Group group : groups) {
            String keyName = buildKeyName(group.keys);
            Map<String, Object> value = Map.of("patterns",
                    group.patterns != null ? group.patterns : Collections.emptyList());
            result.put(keyName, value);
        }

        return result;
    }

    private String buildKeyName(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return "";
        } else if (keys.size() == 1) {
            return keys.get(0);
        } else {
            try {
                return mapper.writeValueAsString(keys);
            } catch (Exception e) {
                return String.join(",", keys);
            }
        }
    }

    // Public API methods
    public Set<String> getKeys() {
        return new LinkedHashSet<>(keys);
    }

    public List<String> getPatterns() {
        return new ArrayList<>(patterns);
    }

    public void reload() {
        loadConfiguration();
    }

    public Map<String, Object> getGroupedConfig() {
        lock.readLock().lock();
        try {
            return buildGroupedMap();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setConfig(Collection<String> newKeys, Collection<String> newPatterns) {
        Group group = new Group();
        group.keys = buildDeduplicatedKeys(newKeys);
        group.patterns = buildValidPatterns(newPatterns);

        updateGroupsAndRecompute(List.of(group));
        persistConfiguration();
    }

    private List<String> buildDeduplicatedKeys(Collection<String> newKeys) {
        LinkedHashSet<String> keySet = new LinkedHashSet<>();

        if (newKeys != null) {
            newKeys.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(keySet::add);
        }

        keySet.addAll(DEFAULT_KEYS);
        return new ArrayList<>(keySet);
    }

    private List<String> buildValidPatterns(Collection<String> newPatterns) {
        if (newPatterns == null) {
            return new ArrayList<>();
        }

        return newPatterns.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    public void setGroupedConfig(Map<String, Object> grouped) {
        lock.writeLock().lock();
        try {
            List<Group> newGroups = parseGroupsFromMap(grouped);
            ensureDefaultKeysPresent(newGroups);
            this.groups = newGroups;
            persistConfiguration();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private List<Group> parseGroupsFromMap(Map<String, Object> grouped) {
        List<Group> newGroups = new ArrayList<>();

        if (grouped != null) {
            for (Map.Entry<String, Object> entry : grouped.entrySet()) {
                try {
                    Group group = parseGroupFromMapEntry(entry);
                    newGroups.add(group);
                } catch (Exception e) {
                    logger.warn("Failed to parse group from entry: {}", entry.getKey(), e);
                }
            }
        }

        if (newGroups.isEmpty()) {
            newGroups.add(createDefaultGroup());
        }

        return newGroups;
    }

    private Group parseGroupFromMapEntry(Map.Entry<String, Object> entry) throws IOException {
        Group group = new Group();
        group.keys = parseKeysFromPropertyName(entry.getKey());

        if (entry.getValue() instanceof Map<?, ?> valueMap) {
            Object patternsObj = valueMap.get("patterns");
            if (patternsObj instanceof Collection<?> patterns) {
                group.patterns = patterns.stream()
                        .filter(Objects::nonNull)
                        .map(String::valueOf)
                        .filter(s -> !s.trim().isEmpty())
                        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            }
        }

        return group;
    }

    private void ensureDefaultKeysPresent(List<Group> groups) {
        boolean hasDefaults = groups.stream()
                .flatMap(g -> g.keys != null ? g.keys.stream() : Stream.empty())
                .anyMatch(key -> DEFAULT_KEYS.contains(key.toLowerCase()));

        if (!hasDefaults && !groups.isEmpty()) {
            Group firstGroup = groups.get(0);
            if (firstGroup.keys == null) firstGroup.keys = new ArrayList<>();
            firstGroup.keys.addAll(DEFAULT_KEYS);
            firstGroup.keys = new ArrayList<>(new LinkedHashSet<>(firstGroup.keys));
        }
    }
}