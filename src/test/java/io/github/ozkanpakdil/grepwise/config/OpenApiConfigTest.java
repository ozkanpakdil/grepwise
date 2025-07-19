package io.github.ozkanpakdil.grepwise.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the OpenAPI documentation configuration.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class OpenApiConfigTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Test that the OpenAPI documentation endpoint is accessible and returns JSON.
     */
    @Test
    public void testOpenApiDocumentationEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"));
    }

    /**
     * Test that the Swagger UI endpoint is accessible.
     */
    @Test
    public void testSwaggerUiEndpoint() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}