package io.github.ozkanpakdil.grepwise.filter;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.ozkanpakdil.grepwise.config.RateLimitingConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Filter for rate limiting API requests.
 * This filter intercepts all HTTP requests and applies rate limiting based on the client's identity.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    private final RateLimitingConfig rateLimitingConfig;

    /**
     * HTTP header for rate limit information.
     */
    public static final String HEADER_LIMIT_REMAINING = "X-Rate-Limit-Remaining";
    
    /**
     * HTTP header for rate limit reset time.
     */
    public static final String HEADER_RETRY_AFTER = "X-Rate-Limit-Retry-After-Seconds";

    public RateLimitingFilter(RateLimitingConfig rateLimitingConfig) {
        this.rateLimitingConfig = rateLimitingConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Skip rate limiting for non-API requests
        String path = request.getRequestURI();
        if (!path.startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Determine client identifier (IP address or user ID)
        String clientId = getClientIdentifier(request);
        
        // Determine bucket type based on the request path
        String bucketType = getBucketType(path);
        
        // Get the appropriate bucket for this client
        Bucket bucket = rateLimitingConfig.resolveBucket(clientId, bucketType);
        
        // Try to consume a token from the bucket
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (probe.isConsumed()) {
            // Request is allowed, add rate limit headers
            response.addHeader(HEADER_LIMIT_REMAINING, String.valueOf(probe.getRemainingTokens()));
            
            // Continue with the request
            filterChain.doFilter(request, response);
        } else {
            // Request is rate limited
            long waitTimeSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            
            // Add rate limit headers
            response.addHeader(HEADER_RETRY_AFTER, String.valueOf(waitTimeSeconds));
            
            // Set response status and body
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Rate limit exceeded. Please try again in " + waitTimeSeconds + " seconds.");
            
            logger.warn("Rate limit exceeded for client: {}, path: {}, retry after: {} seconds", 
                    clientId, path, waitTimeSeconds);
        }
    }

    /**
     * Get the client identifier from the request.
     * This method uses the authenticated user's ID if available, otherwise falls back to the client's IP address.
     * 
     * @param request The HTTP request
     * @return The client identifier
     */
    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        
        // Fall back to IP address
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return clientIp;
    }

    /**
     * Determine the bucket type based on the request path.
     * Different endpoints may have different rate limits.
     * 
     * @param path The request path
     * @return The bucket type (default, search, admin)
     */
    private String getBucketType(String path) {
        // Search endpoints get higher limits
        if (path.contains("/api/logs/search") || path.contains("/api/logs/count")) {
            return "search";
        }
        
        // Admin endpoints get lower limits
        if (path.contains("/api/users") || path.contains("/api/roles") || path.contains("/api/settings")) {
            return "admin";
        }
        
        // Default rate limit for all other endpoints
        return "default";
    }
}