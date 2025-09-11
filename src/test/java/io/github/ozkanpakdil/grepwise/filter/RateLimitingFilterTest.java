package io.github.ozkanpakdil.grepwise.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.ozkanpakdil.grepwise.config.RateLimitingConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RateLimitingFilterTest {

    @Mock
    private RateLimitingConfig rateLimitingConfig;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Bucket bucket;

    @Mock
    private ConsumptionProbe consumptionProbe;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    private StringWriter responseWriter;

    @BeforeEach
    public void setUp() throws Exception {
        // Setup response writer
        responseWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(responseWriter);
        lenient().when(response.getWriter()).thenReturn(writer);

        // Setup security context
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    public void testNonApiRequestsAreNotRateLimited() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/non-api/endpoint");

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(rateLimitingConfig, never()).resolveBucket(anyString(), anyString());
    }

    @Test
    public void testApiRequestsAreRateLimited_Allowed() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/logs/search");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testUser");
        
        when(rateLimitingConfig.resolveBucket(eq("testUser"), eq("search"))).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        when(consumptionProbe.isConsumed()).thenReturn(true);
        when(consumptionProbe.getRemainingTokens()).thenReturn(99L);

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain).doFilter(request, response);
        verify(response).addHeader(eq(RateLimitingFilter.HEADER_LIMIT_REMAINING), eq("99"));
    }

    @Test
    public void testApiRequestsAreRateLimited_Denied() throws Exception {
        // Given
        when(request.getRequestURI()).thenReturn("/api/users");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testUser");
        
        when(rateLimitingConfig.resolveBucket(eq("testUser"), eq("admin"))).thenReturn(bucket);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        when(consumptionProbe.isConsumed()).thenReturn(false);
        when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(5_000_000_000L); // 5 seconds

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(filterChain, never()).doFilter(request, response);
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).addHeader(eq(RateLimitingFilter.HEADER_RETRY_AFTER), eq("5"));
        
        // Verify response body contains retry information
        assertTrue(responseWriter.toString().contains("Rate limit exceeded"));
        assertTrue(responseWriter.toString().contains("5 seconds"));
    }

    @Test
    public void testDifferentBucketTypesForDifferentEndpoints() throws Exception {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testUser");
        when(consumptionProbe.isConsumed()).thenReturn(true);
        when(consumptionProbe.getRemainingTokens()).thenReturn(99L);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);

        // Test search endpoint
        when(request.getRequestURI()).thenReturn("/api/logs/search");
        when(rateLimitingConfig.resolveBucket(eq("testUser"), eq("search"))).thenReturn(bucket);
        
        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(rateLimitingConfig).resolveBucket(eq("testUser"), eq("search"));
        
        // Reset
        reset(rateLimitingConfig, request);
        
        // Test admin endpoint
        when(request.getRequestURI()).thenReturn("/api/users");
        when(rateLimitingConfig.resolveBucket(eq("testUser"), eq("admin"))).thenReturn(bucket);
        
        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(rateLimitingConfig).resolveBucket(eq("testUser"), eq("admin"));
        
        // Reset
        reset(rateLimitingConfig, request);
        
        // Test default endpoint
        when(request.getRequestURI()).thenReturn("/api/other");
        when(rateLimitingConfig.resolveBucket(eq("testUser"), eq("default"))).thenReturn(bucket);
        
        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        
        // Then
        verify(rateLimitingConfig).resolveBucket(eq("testUser"), eq("default"));
    }
}