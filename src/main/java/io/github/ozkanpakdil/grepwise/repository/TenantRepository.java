package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.Tenant;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for storing and retrieving tenant information.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class TenantRepository {
    private final Map<String, Tenant> tenants = new ConcurrentHashMap<>();

    /**
     * Save a tenant.
     *
     * @param tenant The tenant to save
     * @return The saved tenant with a generated ID
     */
    public Tenant save(Tenant tenant) {
        if (tenant.getId() == null || tenant.getId().isEmpty()) {
            tenant.setId(UUID.randomUUID().toString());
        }

        // Set timestamps if not already set
        long now = System.currentTimeMillis();
        if (tenant.getCreatedAt() == 0) {
            tenant.setCreatedAt(now);
        }
        tenant.setUpdatedAt(now);

        tenants.put(tenant.getId(), tenant);
        return tenant;
    }

    /**
     * Find a tenant by ID.
     *
     * @param id The ID of the tenant to find
     * @return The tenant, or null if not found
     */
    public Tenant findById(String id) {
        return tenants.get(id);
    }

    /**
     * Find a tenant by name.
     *
     * @param name The name to find
     * @return The tenant, or null if not found
     */
    public Tenant findByName(String name) {
        return tenants.values().stream()
                .filter(tenant -> name.equals(tenant.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find all tenants.
     *
     * @return A list of all tenants
     */
    public List<Tenant> findAll() {
        return new ArrayList<>(tenants.values());
    }

    /**
     * Find active tenants.
     *
     * @return A list of active tenants
     */
    public List<Tenant> findByActive(boolean active) {
        return tenants.values().stream()
                .filter(tenant -> tenant.isActive() == active)
                .collect(Collectors.toList());
    }

    /**
     * Delete a tenant by ID.
     *
     * @param id The ID of the tenant to delete
     * @return true if the tenant was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return tenants.remove(id) != null;
    }

    /**
     * Check if a tenant name already exists.
     *
     * @param name The name to check
     * @return true if the name exists, false otherwise
     */
    public boolean existsByName(String name) {
        return tenants.values().stream()
                .anyMatch(tenant -> name.equals(tenant.getName()));
    }

    /**
     * Get the total number of tenants.
     *
     * @return The total number of tenants
     */
    public int count() {
        return tenants.size();
    }
}