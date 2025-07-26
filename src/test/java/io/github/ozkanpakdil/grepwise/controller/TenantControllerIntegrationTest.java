package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.Tenant;
import io.github.ozkanpakdil.grepwise.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the TenantController.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class TenantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    private Tenant testTenant;

    @BeforeEach
    public void setUp() {
        // Clear any existing tenants
        for (Tenant tenant : tenantRepository.findAll()) {
            tenantRepository.deleteById(tenant.getId());
        }

        // Create a test tenant
        testTenant = new Tenant();
        testTenant.setName("Test Tenant");
        testTenant.setDescription("Test Tenant Description");
        testTenant.setActive(true);
        testTenant = tenantRepository.save(testTenant);
    }

    @Test
    public void testGetAllTenants() throws Exception {
        mockMvc.perform(get("/api/tenants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Test Tenant")))
                .andExpect(jsonPath("$[0].description", is("Test Tenant Description")))
                .andExpect(jsonPath("$[0].active", is(true)));
    }

    @Test
    public void testGetTenantById() throws Exception {
        mockMvc.perform(get("/api/tenants/{id}", testTenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Test Tenant")))
                .andExpect(jsonPath("$.description", is("Test Tenant Description")))
                .andExpect(jsonPath("$.active", is(true)));
    }

    @Test
    public void testGetTenantById_NotFound() throws Exception {
        mockMvc.perform(get("/api/tenants/non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateTenant() throws Exception {
        String tenantJson = "{"
                + "\"name\": \"New Tenant\","
                + "\"description\": \"New Tenant Description\","
                + "\"active\": true"
                + "}";

        MvcResult result = mockMvc.perform(post("/api/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("New Tenant")))
                .andExpect(jsonPath("$.description", is("New Tenant Description")))
                .andExpect(jsonPath("$.active", is(true)))
                .andReturn();

        // Verify the tenant was created in the repository
        String responseJson = result.getResponse().getContentAsString();
        String tenantId = responseJson.split("\"id\":\"")[1].split("\"")[0];
        Tenant createdTenant = tenantRepository.findById(tenantId);
        assertNotNull(createdTenant);
        assertEquals("New Tenant", createdTenant.getName());
    }

    @Test
    public void testCreateTenant_NameAlreadyExists() throws Exception {
        String tenantJson = "{"
                + "\"name\": \"Test Tenant\","
                + "\"description\": \"Duplicate Tenant\","
                + "\"active\": true"
                + "}";

        mockMvc.perform(post("/api/tenants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("already exists")));
    }

    @Test
    public void testUpdateTenant() throws Exception {
        String tenantJson = "{"
                + "\"name\": \"Updated Tenant\","
                + "\"description\": \"Updated Description\","
                + "\"active\": false"
                + "}";

        mockMvc.perform(put("/api/tenants/{id}", testTenant.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Tenant")))
                .andExpect(jsonPath("$.description", is("Updated Description")))
                .andExpect(jsonPath("$.active", is(false)));

        // Verify the tenant was updated in the repository
        Tenant updatedTenant = tenantRepository.findById(testTenant.getId());
        assertNotNull(updatedTenant);
        assertEquals("Updated Tenant", updatedTenant.getName());
        assertEquals("Updated Description", updatedTenant.getDescription());
        assertFalse(updatedTenant.isActive());
    }

    @Test
    public void testUpdateTenant_NotFound() throws Exception {
        String tenantJson = "{"
                + "\"name\": \"Updated Tenant\","
                + "\"description\": \"Updated Description\","
                + "\"active\": false"
                + "}";

        mockMvc.perform(put("/api/tenants/non-existent-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(tenantJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("not found")));
    }

    @Test
    public void testDeleteTenant() throws Exception {
        mockMvc.perform(delete("/api/tenants/{id}", testTenant.getId()))
                .andExpect(status().isNoContent());

        // Verify the tenant was deleted from the repository
        Tenant deletedTenant = tenantRepository.findById(testTenant.getId());
        assertNull(deletedTenant);
    }

    @Test
    public void testDeleteTenant_NotFound() throws Exception {
        mockMvc.perform(delete("/api/tenants/non-existent-id"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testActivateTenant() throws Exception {
        // First deactivate the tenant
        testTenant.setActive(false);
        tenantRepository.save(testTenant);

        mockMvc.perform(put("/api/tenants/{id}/activate", testTenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(true)));

        // Verify the tenant was activated in the repository
        Tenant activatedTenant = tenantRepository.findById(testTenant.getId());
        assertNotNull(activatedTenant);
        assertTrue(activatedTenant.isActive());
    }

    @Test
    public void testDeactivateTenant() throws Exception {
        mockMvc.perform(put("/api/tenants/{id}/deactivate", testTenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active", is(false)));

        // Verify the tenant was deactivated in the repository
        Tenant deactivatedTenant = tenantRepository.findById(testTenant.getId());
        assertNotNull(deactivatedTenant);
        assertFalse(deactivatedTenant.isActive());
    }
}