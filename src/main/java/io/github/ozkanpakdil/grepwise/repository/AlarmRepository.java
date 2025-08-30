package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.Alarm;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for storing and retrieving alarm information.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class AlarmRepository {
    private final Map<String, Alarm> alarms = new ConcurrentHashMap<>();

    /**
     * Save an alarm.
     *
     * @param alarm The alarm to save
     * @return The saved alarm with a generated ID
     */
    public Alarm save(Alarm alarm) {
        if (alarm.getId() == null || alarm.getId().isEmpty()) {
            alarm.setId(UUID.randomUUID().toString());
        }

        // Set timestamps if not already set
        long now = System.currentTimeMillis();
        if (alarm.getCreatedAt() == 0) {
            alarm.setCreatedAt(now);
        }
        alarm.setUpdatedAt(now);

        alarms.put(alarm.getId(), alarm);
        return alarm;
    }

    /**
     * Find an alarm by ID.
     *
     * @param id The ID of the alarm to find
     * @return The alarm, or null if not found
     */
    public Alarm findById(String id) {
        return alarms.get(id);
    }

    /**
     * Find all alarms.
     *
     * @return A list of all alarms
     */
    public List<Alarm> findAll() {
        return new ArrayList<>(alarms.values());
    }

    /**
     * Find alarms by enabled status.
     *
     * @param enabled The enabled status to filter by
     * @return A list of alarms with the specified enabled status
     */
    public List<Alarm> findByEnabled(Boolean enabled) {
        return alarms.values().stream()
                .filter(alarm -> enabled.equals(alarm.getEnabled()))
                .collect(Collectors.toList());
    }

    /**
     * Find alarms by name (case-insensitive partial match).
     *
     * @param name The name to search for
     * @return A list of alarms with names containing the specified text
     */
    public List<Alarm> findByNameContaining(String name) {
        return alarms.values().stream()
                .filter(alarm -> alarm.getName() != null &&
                        alarm.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Delete an alarm by ID.
     *
     * @param id The ID of the alarm to delete
     * @return true if the alarm was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return alarms.remove(id) != null;
    }

    /**
     * Check if an alarm with the given name already exists.
     *
     * @param name The name to check
     * @return true if an alarm with the name exists, false otherwise
     */
    public boolean existsByName(String name) {
        return alarms.values().stream()
                .anyMatch(alarm -> name.equals(alarm.getName()));
    }

    /**
     * Get the total number of alarms.
     *
     * @return The total number of alarms
     */
    public int count() {
        return alarms.size();
    }

    /**
     * Get the number of enabled alarms.
     *
     * @return The number of enabled alarms
     */
    public int countEnabled() {
        return (int) alarms.values().stream()
                .filter(alarm -> Boolean.TRUE.equals(alarm.getEnabled()))
                .count();
    }

    /**
     * Update an existing alarm.
     *
     * @param alarm The alarm to update
     * @return The updated alarm, or null if the alarm doesn't exist
     */
    public Alarm update(Alarm alarm) {
        if (alarm.getId() == null || !alarms.containsKey(alarm.getId())) {
            return null;
        }

        alarm.setUpdatedAt(System.currentTimeMillis());
        alarms.put(alarm.getId(), alarm);
        return alarm;
    }
}