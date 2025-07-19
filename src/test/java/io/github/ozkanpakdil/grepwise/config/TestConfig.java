package io.github.ozkanpakdil.grepwise.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.ozkanpakdil.grepwise.filter.RateLimitingFilter;
import io.github.ozkanpakdil.grepwise.model.User;
import io.github.ozkanpakdil.grepwise.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test configuration that provides mock implementations of beans for tests.
 * This configuration is used to replace real implementations with test doubles
 * to avoid dependencies on external systems and to simplify test setup.
 */
@TestConfiguration
public class TestConfig {

    /**
     * Provides a mock implementation of RateLimitingFilter for tests.
     * This implementation simply passes requests through without rate limiting.
     *
     * @return A mock RateLimitingFilter
     */
    @Bean
    @Primary
    public RateLimitingFilter rateLimitingFilter() {
        return new MockRateLimitingFilter();
    }

    /**
     * Provides a mock implementation of RateLimitingConfig for tests.
     * This implementation provides dummy buckets that always allow requests.
     *
     * @return A mock RateLimitingConfig
     */
    @Bean
    @Primary
    public RateLimitingConfig rateLimitingConfig() {
        return new MockRateLimitingConfig();
    }

    /**
     * Provides a mock implementation of TokenService for tests.
     * This implementation provides dummy token functionality.
     *
     * @return A mock TokenService
     */
    @Bean
    @Primary
    public TokenService tokenService() {
        return new MockTokenService();
    }

    /**
     * Mock implementation of RateLimitingFilter for tests.
     * This implementation simply passes requests through without rate limiting.
     */
    public static class MockRateLimitingFilter extends RateLimitingFilter {
        public MockRateLimitingFilter() {
            super(new MockRateLimitingConfig());
        }
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            // Simply pass the request to the next filter in the chain
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Mock implementation of RateLimitingConfig for tests.
     * This implementation provides dummy buckets that always allow requests.
     */
    public static class MockRateLimitingConfig extends RateLimitingConfig {
        private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();
        
        /**
         * Creates a dummy bucket that always allows requests.
         */
        private Bucket createDummyBucket() {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(Integer.MAX_VALUE, Refill.intervally(Integer.MAX_VALUE, Duration.ofSeconds(1))))
                    .build();
        }
        
        @Override
        public Bucket defaultBucket() {
            return createDummyBucket();
        }
        
        @Override
        public Bucket searchBucket() {
            return createDummyBucket();
        }
        
        @Override
        public Bucket adminBucket() {
            return createDummyBucket();
        }
        
        @Override
        public Bucket resolveBucket(String clientId, String bucketType) {
            return bucketCache.computeIfAbsent(clientId, key -> createDummyBucket());
        }
    }

    /**
     * Mock implementation of TokenService for tests.
     * This implementation provides dummy token functionality.
     */
    public static class MockTokenService extends TokenService {
        private static final String DUMMY_TOKEN = "dummy.jwt.token";
        
        @Override
        public String generateToken(User user) {
            return DUMMY_TOKEN;
        }
        
        @Override
        public String generateRefreshToken(User user) {
            return DUMMY_TOKEN;
        }
        
        @Override
        public boolean validateToken(String token) {
            return true;
        }
        
        @Override
        public String getUserIdFromToken(String token) {
            return "test-user-id";
        }
        
        @Override
        public String getUsernameFromToken(String token) {
            return "test-user";
        }
        
        @Override
        public List<String> getRolesFromToken(String token) {
            List<String> roles = new ArrayList<>();
            roles.add("ROLE_USER");
            return roles;
        }
        
        @Override
        public Date getExpirationDateFromToken(String token) {
            return new Date(System.currentTimeMillis() + 3600000); // 1 hour from now
        }
        
        @Override
        public boolean isTokenExpired(String token) {
            return false;
        }
    }
}