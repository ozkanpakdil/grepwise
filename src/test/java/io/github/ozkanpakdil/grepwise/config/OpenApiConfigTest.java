package io.github.ozkanpakdil.grepwise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for the OpenApiConfig class.
 * These tests verify that the OpenAPI configuration is correctly set up
 * without requiring a full Spring context.
 */
public class OpenApiConfigTest {

    /**
     * Test that the OpenAPI configuration is correctly set up.
     */
    @Test
    public void testOpenApiConfiguration() {
        // Create an instance of the configuration class
        OpenApiConfig config = new OpenApiConfig();
        
        // Get the OpenAPI bean
        OpenAPI openAPI = config.grepWiseOpenAPI();
        
        // Verify the OpenAPI configuration
        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("GrepWise API", openAPI.getInfo().getTitle());
        assertEquals("v1.0.0", openAPI.getInfo().getVersion());
        
        // Verify contact information
        Contact contact = openAPI.getInfo().getContact();
        assertNotNull(contact);
        assertEquals("GrepWise Team", contact.getName());
        assertEquals("https://github.com/ozkanpakdil/GrepWise", contact.getUrl());
        assertEquals("info@grepwise.org", contact.getEmail());
        
        // Verify license information
        License license = openAPI.getInfo().getLicense();
        assertNotNull(license);
        assertEquals("MIT License", license.getName());
        assertEquals("https://opensource.org/licenses/MIT", license.getUrl());
        
        // Verify servers
        List<Server> servers = openAPI.getServers();
        assertNotNull(servers);
        assertEquals(2, servers.size());
        assertEquals("http://localhost:8080", servers.get(0).getUrl());
        assertEquals("https://api.grepwise.org", servers.get(1).getUrl());
    }
}