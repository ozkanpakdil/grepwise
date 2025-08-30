package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.Tenant;
import io.github.ozkanpakdil.grepwise.service.TenantService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for managing tenants in a multi-tenant system.
 */
@RestController
@RequestMapping("/api/tenants")
public class TenantController {
    private static final Logger logger = LoggerFactory.getLogger(TenantController.class);

    private final TenantService tenantService;

    @Autowired
    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * Get all tenants.
     *
     * @return A list of all tenants
     */
    @GetMapping
    public ResponseEntity<List<TenantResponse>> getAllTenants() {
        List<Tenant> tenants = tenantService.getAllTenants();
        List<TenantResponse> response = tenants.stream()
                .map(this::convertToTenantResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get active tenants.
     *
     * @return A list of active tenants
     */
    @GetMapping("/active")
    public ResponseEntity<List<TenantResponse>> getActiveTenants() {
        List<Tenant> tenants = tenantService.getActiveTenants();
        List<TenantResponse> response = tenants.stream()
                .map(this::convertToTenantResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Get a tenant by ID.
     *
     * @param id The ID of the tenant to get
     * @return The tenant, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponse> getTenantById(@PathVariable String id) {
        Tenant tenant = tenantService.getTenantById(id);
        if (tenant == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(convertToTenantResponse(tenant));
    }

    /**
     * Create a new tenant.
     *
     * @param tenantRequest The tenant data
     * @return The created tenant
     */
    @PostMapping
    public ResponseEntity<?> createTenant(@RequestBody TenantRequest tenantRequest) {
        try {
            Tenant tenant = new Tenant();
            tenant.setName(tenantRequest.getName());
            tenant.setDescription(tenantRequest.getDescription());
            tenant.setActive(tenantRequest.isActive());

            Tenant createdTenant = tenantService.createTenant(tenant);
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToTenantResponse(createdTenant));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating tenant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error creating tenant: " + e.getMessage());
        }
    }

    /**
     * Update an existing tenant.
     *
     * @param id            The ID of the tenant to update
     * @param tenantRequest The updated tenant data
     * @return The updated tenant
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTenant(@PathVariable String id, @RequestBody TenantRequest tenantRequest) {
        try {
            Tenant tenant = new Tenant();
            tenant.setName(tenantRequest.getName());
            tenant.setDescription(tenantRequest.getDescription());
            tenant.setActive(tenantRequest.isActive());

            Tenant updatedTenant = tenantService.updateTenant(id, tenant);
            return ResponseEntity.ok(convertToTenantResponse(updatedTenant));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating tenant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error updating tenant: " + e.getMessage());
        }
    }

    /**
     * Delete a tenant by ID.
     *
     * @param id The ID of the tenant to delete
     * @return 204 No Content if successful, 404 if not found
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTenant(@PathVariable String id) {
        try {
            boolean deleted = tenantService.deleteTenant(id);
            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting tenant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting tenant: " + e.getMessage());
        }
    }

    /**
     * Activate a tenant.
     *
     * @param id The ID of the tenant to activate
     * @return The activated tenant
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateTenant(@PathVariable String id) {
        try {
            Tenant activatedTenant = tenantService.setTenantActive(id, true);
            return ResponseEntity.ok(convertToTenantResponse(activatedTenant));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error activating tenant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error activating tenant: " + e.getMessage());
        }
    }

    /**
     * Deactivate a tenant.
     *
     * @param id The ID of the tenant to deactivate
     * @return The deactivated tenant
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivateTenant(@PathVariable String id) {
        try {
            Tenant deactivatedTenant = tenantService.setTenantActive(id, false);
            return ResponseEntity.ok(convertToTenantResponse(deactivatedTenant));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            logger.error("Error deactivating tenant", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deactivating tenant: " + e.getMessage());
        }
    }

    /**
     * Convert a Tenant entity to a TenantResponse DTO.
     *
     * @param tenant The Tenant entity to convert
     * @return The TenantResponse DTO
     */
    private TenantResponse convertToTenantResponse(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setName(tenant.getName());
        response.setDescription(tenant.getDescription());
        response.setActive(tenant.isActive());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        return response;
    }

    /**
     * Request DTO for tenant operations.
     */
    public static class TenantRequest {
        private String name;
        private String description;
        private boolean active = true;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    /**
     * Response DTO for tenant operations.
     */
    public static class TenantResponse {
        private String id;
        private String name;
        private String description;
        private boolean active;
        private long createdAt;
        private long updatedAt;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}