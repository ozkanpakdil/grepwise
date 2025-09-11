package io.github.ozkanpakdil.grepwise.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true", "gg.jte.developmentMode=true", "management.health.ldap.enabled=false"})
@AutoConfigureMockMvc
@org.springframework.test.context.ActiveProfiles("test")
@org.springframework.context.annotation.Import(io.github.ozkanpakdil.grepwise.config.TestConfig.class)
public class ApiDocumentationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApiDocumentationController apiDocumentationController;

    @Test
    void testApiDocumentationControllerExists() {
        assertNotNull(apiDocumentationController);
    }

    @Test
    void testGetApiDocumentationHtml() throws Exception {
        mockMvc.perform(get("/api-docs")
                .accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }

    @Test
    void testGetApiEndpointsJson() throws Exception {
        MvcResult result = mockMvc.perform(get("/api-docs")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertNotNull(content);
        assertTrue(content.contains("GrepWise API Documentation"));
    }

    @Test
    void testApiEndpointsContainsAllControllers() {
        Map<String, Object> apiEndpoints = apiDocumentationController.getApiEndpoints();
        assertNotNull(apiEndpoints);
        
        // Check that the response contains the expected keys
        assertTrue(apiEndpoints.containsKey("title"));
        assertTrue(apiEndpoints.containsKey("version"));
        assertTrue(apiEndpoints.containsKey("description"));
        assertTrue(apiEndpoints.containsKey("endpoints"));
        
        // Check that the endpoints list is not empty
        assertNotNull(apiEndpoints.get("endpoints"));
        assertTrue(((java.util.List<?>) apiEndpoints.get("endpoints")).size() > 0);
        
        // Verify that endpoints from all controllers are included
        String endpointsStr = apiEndpoints.toString();
        
        // Log Search Controller endpoints
        assertTrue(endpointsStr.contains("/api/logs/search"));
        assertTrue(endpointsStr.contains("/api/logs/spl"));
        
        // Alarm Controller endpoints
        assertTrue(endpointsStr.contains("/api/alarms"));
        
        // Dashboard Controller endpoints
        assertTrue(endpointsStr.contains("/api/dashboards"));
        
        // Log Directory Config Controller endpoints
        assertTrue(endpointsStr.contains("/api/config/log-directories"));
    }
}