package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.Tenant;
import io.github.ozkanpakdil.grepwise.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the TenantService class.
 */
public class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private TenantService tenantService;

    private Tenant testTenant;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        // Create a test tenant
        testTenant = new Tenant();
        testTenant.setId("test-tenant-id");
        testTenant.setName("Test Tenant");
        testTenant.setDescription("Test Tenant Description");
        testTenant.setActive(true);
        testTenant.setCreatedAt(System.currentTimeMillis());
        testTenant.setUpdatedAt(System.currentTimeMillis());
    }

    @Test
    public void testCreateTenant() {
        // Setup
        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(testTenant);
        when(auditLogService.createAuditLog(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any())).thenReturn(new io.github.ozkanpakdil.grepwise.model.AuditLog());

        // Execute
        Tenant result = tenantService.createTenant(testTenant);

        // Verify
        assertNotNull(result);
        assertEquals("test-tenant-id", result.getId());
        assertEquals("Test Tenant", result.getName());
        assertEquals("Test Tenant Description", result.getDescription());
        assertTrue(result.isActive());

        verify(tenantRepository).existsByName("Test Tenant");
        verify(tenantRepository).save(testTenant);
        verify(auditLogService).createAuditLog(eq("TENANT"), eq("CREATE"), eq("SUCCESS"), anyString(), eq("test-tenant-id"), eq("TENANT"), isNull());
    }

    @Test
    public void testCreateTenant_NameAlreadyExists() {
        // Setup
        when(tenantRepository.existsByName(anyString())).thenReturn(true);

        // Execute & Verify
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tenantService.createTenant(testTenant);
        });

        assertEquals("Tenant with name 'Test Tenant' already exists", exception.getMessage());
        verify(tenantRepository).existsByName("Test Tenant");
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    public void testGetTenantById() {
        // Setup
        when(tenantRepository.findById(anyString())).thenReturn(testTenant);

        // Execute
        Tenant result = tenantService.getTenantById("test-tenant-id");

        // Verify
        assertNotNull(result);
        assertEquals("test-tenant-id", result.getId());
        verify(tenantRepository).findById("test-tenant-id");
    }

    @Test
    public void testGetTenantByName() {
        // Setup
        when(tenantRepository.findByName(anyString())).thenReturn(testTenant);

        // Execute
        Tenant result = tenantService.getTenantByName("Test Tenant");

        // Verify
        assertNotNull(result);
        assertEquals("Test Tenant", result.getName());
        verify(tenantRepository).findByName("Test Tenant");
    }

    @Test
    public void testGetAllTenants() {
        // Setup
        Tenant tenant2 = new Tenant();
        tenant2.setId("tenant-2");
        tenant2.setName("Tenant 2");

        List<Tenant> tenants = Arrays.asList(testTenant, tenant2);
        when(tenantRepository.findAll()).thenReturn(tenants);

        // Execute
        List<Tenant> result = tenantService.getAllTenants();

        // Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(tenantRepository).findAll();
    }

    @Test
    public void testGetActiveTenants() {
        // Setup
        Tenant tenant2 = new Tenant();
        tenant2.setId("tenant-2");
        tenant2.setName("Tenant 2");
        tenant2.setActive(true);

        List<Tenant> tenants = Arrays.asList(testTenant, tenant2);
        when(tenantRepository.findByActive(true)).thenReturn(tenants);

        // Execute
        List<Tenant> result = tenantService.getActiveTenants();

        // Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(tenantRepository).findByActive(true);
    }

    @Test
    public void testUpdateTenant() {
        // Setup
        Tenant updatedTenant = new Tenant();
        updatedTenant.setName("Updated Tenant");
        updatedTenant.setDescription("Updated Description");
        updatedTenant.setActive(false);

        when(tenantRepository.findById(anyString())).thenReturn(testTenant);
        when(tenantRepository.existsByName(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant savedTenant = invocation.getArgument(0);
            savedTenant.setId("test-tenant-id");
            savedTenant.setCreatedAt(testTenant.getCreatedAt());
            savedTenant.setUpdatedAt(System.currentTimeMillis());
            return savedTenant;
        });

        // Execute
        Tenant result = tenantService.updateTenant("test-tenant-id", updatedTenant);

        // Verify
        assertNotNull(result);
        assertEquals("test-tenant-id", result.getId());
        assertEquals("Updated Tenant", result.getName());
        assertEquals("Updated Description", result.getDescription());
        assertFalse(result.isActive());
        assertEquals(testTenant.getCreatedAt(), result.getCreatedAt());

        verify(tenantRepository).findById("test-tenant-id");
        verify(tenantRepository).existsByName("Updated Tenant");
        verify(tenantRepository).save(any(Tenant.class));
        verify(auditLogService).createAuditLog(eq("TENANT"), eq("UPDATE"), eq("SUCCESS"), anyString(), eq("test-tenant-id"), eq("TENANT"), isNull());
    }

    @Test
    public void testUpdateTenant_NotFound() {
        // Setup
        when(tenantRepository.findById(anyString())).thenReturn(null);

        // Execute & Verify
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tenantService.updateTenant("non-existent-id", testTenant);
        });

        assertEquals("Tenant with ID 'non-existent-id' not found", exception.getMessage());
        verify(tenantRepository).findById("non-existent-id");
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    public void testUpdateTenant_NameAlreadyExists() {
        // Setup
        Tenant existingTenant = new Tenant();
        existingTenant.setId("test-tenant-id");
        existingTenant.setName("Existing Tenant");

        Tenant updatedTenant = new Tenant();
        updatedTenant.setName("Another Tenant");

        when(tenantRepository.findById(anyString())).thenReturn(existingTenant);
        when(tenantRepository.existsByName("Another Tenant")).thenReturn(true);

        // Execute & Verify
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tenantService.updateTenant("test-tenant-id", updatedTenant);
        });

        assertEquals("Tenant with name 'Another Tenant' already exists", exception.getMessage());
        verify(tenantRepository).findById("test-tenant-id");
        verify(tenantRepository).existsByName("Another Tenant");
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    public void testDeleteTenant() {
        // Setup
        when(tenantRepository.findById(anyString())).thenReturn(testTenant);
        when(tenantRepository.deleteById(anyString())).thenReturn(true);

        // Execute
        boolean result = tenantService.deleteTenant("test-tenant-id");

        // Verify
        assertTrue(result);
        verify(tenantRepository).findById("test-tenant-id");
        verify(tenantRepository).deleteById("test-tenant-id");
        verify(auditLogService).createAuditLog(eq("TENANT"), eq("DELETE"), eq("SUCCESS"), anyString(), eq("test-tenant-id"), eq("TENANT"), isNull());
    }

    @Test
    public void testDeleteTenant_NotFound() {
        // Setup
        when(tenantRepository.findById(anyString())).thenReturn(null);

        // Execute
        boolean result = tenantService.deleteTenant("non-existent-id");

        // Verify
        assertFalse(result);
        verify(tenantRepository).findById("non-existent-id");
        verify(tenantRepository, never()).deleteById(anyString());
    }

    @Test
    public void testSetTenantActive() {
        // Setup
        when(tenantRepository.findById(anyString())).thenReturn(testTenant);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(testTenant);

        // Execute
        Tenant result = tenantService.setTenantActive("test-tenant-id", false);

        // Verify
        assertNotNull(result);
        assertFalse(result.isActive());
        verify(tenantRepository).findById("test-tenant-id");
        verify(tenantRepository).save(testTenant);
        verify(auditLogService).createAuditLog(eq("TENANT"), eq("DEACTIVATE"), eq("SUCCESS"), anyString(), eq("test-tenant-id"), eq("TENANT"), isNull());
    }

    @Test
    public void testSetTenantActive_NotFound() {
        // Setup
        when(tenantRepository.findById(anyString())).thenReturn(null);

        // Execute & Verify
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            tenantService.setTenantActive("non-existent-id", true);
        });

        assertEquals("Tenant with ID 'non-existent-id' not found", exception.getMessage());
        verify(tenantRepository).findById("non-existent-id");
        verify(tenantRepository, never()).save(any(Tenant.class));
    }

    @Test
    public void testGetTenantCount() {
        // Setup
        when(tenantRepository.count()).thenReturn(5);

        // Execute
        int result = tenantService.getTenantCount();

        // Verify
        assertEquals(5, result);
        verify(tenantRepository).count();
    }
}