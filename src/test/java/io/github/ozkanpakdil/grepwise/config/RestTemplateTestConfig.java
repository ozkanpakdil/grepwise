package io.github.ozkanpakdil.grepwise.config;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Test configuration for RestTemplate.
 * This configuration provides a mock RestTemplate for tests to avoid making real HTTP requests.
 */
@Configuration
public class RestTemplateTestConfig {

    /**
     * Creates a mock RestTemplate bean for tests.
     * This mock will return successful responses for any requests.
     *
     * @return A mock RestTemplate
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate mockRestTemplate = Mockito.mock(RestTemplate.class);
        
        // Configure the mock to return a successful response for any postForEntity call
        ResponseEntity<Map> successResponse = new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
        Mockito.when(mockRestTemplate.postForEntity(
            Mockito.anyString(), 
            Mockito.any(), 
            Mockito.eq(Map.class)
        )).thenReturn(successResponse);
        
        return mockRestTemplate;
    }
}