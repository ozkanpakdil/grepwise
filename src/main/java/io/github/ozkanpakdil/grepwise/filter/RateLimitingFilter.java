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
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Filter for rate limiting API requests.
 * This filter intercepts all HTTP requests and applies rate limiting based on the client's identity.
 */
public class RateLimitingFilter extends OncePerRequestFilter {

    /**
     * HTTP header for rate limit information.
     */
    public static final String HEADER_LIMIT_REMAINING = "X-Rate-Limit-Remaining";
    /**
     * HTTP header for rate limit reset time.
     */
    public static final String HEADER_RETRY_AFTER = "X-Rate-Limit-Retry-After-Seconds";
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    private final RateLimitingConfig rateLimitingConfig;

    public RateLimitingFilter(RateLimitingConfig rateLimitingConfig) {
        this.rateLimitingConfig = rateLimitingConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip rate limiting for non-API requests
        String path = request.getRequestURI();
        String method = request.getMethod();
        String accept = request.getHeader("Accept");
        var dispatcher = request.getDispatcherType();
        if (logger.isDebugEnabled()) {
            logger.debug("[RateLimit] Incoming request path={}, method={}, dispatcher={}, accept={}", path, method, dispatcher, accept);
        }
        if (!path.startsWith("/api/")) {
            if (logger.isDebugEnabled()) {
                logger.debug("[RateLimit] Skipping (non-API) path={}", path);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Skip rate limiting for Server-Sent Events or streaming endpoints
        if (accept != null && accept.contains("text/event-stream")) {
            if (logger.isDebugEnabled()) {
                logger.debug("[RateLimit] Skipping (event-stream) path={}", path);
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Prevent infinite loop - check if this filter has already been applied
        if (request.getAttribute("rateLimitingFilter.applied") != null) {
            filterChain.doFilter(request, response);
            return;
        }
        request.setAttribute("rateLimitingFilter.applied", true);

        try {
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
        } catch (Exception e) {
            logger.error("Error in rate limiting filter", e);
            // Continue with the request on error to avoid breaking the application
            filterChain.doFilter(request, response);
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

    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        // Skip this filter during ERROR dispatch to avoid recursive forwarding/session calls
        return true;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // Skip this filter during ASYNC dispatch; this filter is only for main request thread
        return true;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        // In addition to ERROR/ASYNC dispatch skipping, also skip FORWARD and INCLUDE dispatches
        var dispatcherType = request.getDispatcherType();
        switch (dispatcherType) {
            case ERROR:
            case ASYNC:
            case FORWARD:
            case INCLUDE:
                return true;
            default:
                return false;
        }
    }
}