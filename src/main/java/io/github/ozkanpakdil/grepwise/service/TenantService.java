package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.Tenant;
import io.github.ozkanpakdil.grepwise.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing tenants in a multi-tenant system.
 */
@Service
public class TenantService {
    private static final Logger logger = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository tenantRepository;
    private final AuditLogService auditLogService;

    @Autowired
    public TenantService(TenantRepository tenantRepository, AuditLogService auditLogService) {
        this.tenantRepository = tenantRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Create a new tenant.
     *
     * @param tenant The tenant to create
     * @return The created tenant
     * @throws IllegalArgumentException if a tenant with the same name already exists
     */
    public Tenant createTenant(Tenant tenant) {
        if (tenantRepository.existsByName(tenant.getName())) {
            throw new IllegalArgumentException("Tenant with name '" + tenant.getName() + "' already exists");
        }

        Tenant savedTenant = tenantRepository.save(tenant);
        logger.info("Created tenant: {}", savedTenant.getName());
        auditLogService.createAuditLog(
                "TENANT",
                "CREATE",
                "SUCCESS",
                "Created tenant: " + savedTenant.getName(),
                savedTenant.getId(),
                "TENANT",
                null
        );
        return savedTenant;
    }

    /**
     * Get a tenant by ID.
     *
     * @param id The ID of the tenant to get
     * @return The tenant, or null if not found
     */
    public Tenant getTenantById(String id) {
        return tenantRepository.findById(id);
    }

    /**
     * Get a tenant by name.
     *
     * @param name The name of the tenant to get
     * @return The tenant, or null if not found
     */
    public Tenant getTenantByName(String name) {
        return tenantRepository.findByName(name);
    }

    /**
     * Get all tenants.
     *
     * @return A list of all tenants
     */
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    /**
     * Get active tenants.
     *
     * @return A list of active tenants
     */
    public List<Tenant> getActiveTenants() {
        return tenantRepository.findByActive(true);
    }

    /**
     * Update an existing tenant.
     *
     * @param id     The ID of the tenant to update
     * @param tenant The updated tenant data
     * @return The updated tenant
     * @throws IllegalArgumentException if the tenant doesn't exist or if the name is already taken by another tenant
     */
    public Tenant updateTenant(String id, Tenant tenant) {
        Tenant existingTenant = tenantRepository.findById(id);
        if (existingTenant == null) {
            throw new IllegalArgumentException("Tenant with ID '" + id + "' not found");
        }

        // Check if the name is already taken by another tenant
        if (!existingTenant.getName().equals(tenant.getName()) && tenantRepository.existsByName(tenant.getName())) {
            throw new IllegalArgumentException("Tenant with name '" + tenant.getName() + "' already exists");
        }

        // Update the tenant
        tenant.setId(id);
        tenant.setCreatedAt(existingTenant.getCreatedAt());
        Tenant updatedTenant = tenantRepository.save(tenant);

        logger.info("Updated tenant: {}", updatedTenant.getName());
        auditLogService.createAuditLog(
                "TENANT",
                "UPDATE",
                "SUCCESS",
                "Updated tenant: " + updatedTenant.getName(),
                updatedTenant.getId(),
                "TENANT",
                null
        );

        return updatedTenant;
    }

    /**
     * Delete a tenant by ID.
     *
     * @param id The ID of the tenant to delete
     * @return true if the tenant was deleted, false otherwise
     */
    public boolean deleteTenant(String id) {
        Tenant tenant = tenantRepository.findById(id);
        if (tenant == null) {
            return false;
        }

        boolean deleted = tenantRepository.deleteById(id);
        if (deleted) {
            logger.info("Deleted tenant: {}", tenant.getName());
            auditLogService.createAuditLog(
                    "TENANT",
                    "DELETE",
                    "SUCCESS",
                    "Deleted tenant: " + tenant.getName(),
                    id,
                    "TENANT",
                    null
            );
        }

        return deleted;
    }

    /**
     * Activate or deactivate a tenant.
     *
     * @param id     The ID of the tenant
     * @param active Whether the tenant should be active
     * @return The updated tenant
     * @throws IllegalArgumentException if the tenant doesn't exist
     */
    public Tenant setTenantActive(String id, boolean active) {
        Tenant tenant = tenantRepository.findById(id);
        if (tenant == null) {
            throw new IllegalArgumentException("Tenant with ID '" + id + "' not found");
        }

        tenant.setActive(active);
        Tenant updatedTenant = tenantRepository.save(tenant);

        String status = active ? "activated" : "deactivated";
        String action = active ? "ACTIVATE" : "DEACTIVATE";
        logger.info("{} tenant: {}", status, updatedTenant.getName());
        auditLogService.createAuditLog(
                "TENANT",
                action,
                "SUCCESS",
                status + " tenant: " + updatedTenant.getName(),
                updatedTenant.getId(),
                "TENANT",
                null
        );

        return updatedTenant;
    }

    /**
     * Get the total number of tenants.
     *
     * @return The total number of tenants
     */
    public int getTenantCount() {
        return tenantRepository.count();
    }
}