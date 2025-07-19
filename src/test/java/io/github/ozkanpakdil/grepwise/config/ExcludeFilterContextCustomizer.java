package io.github.ozkanpakdil.grepwise.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.ozkanpakdil.grepwise.filter.RateLimitingFilter;
import io.github.ozkanpakdil.grepwise.model.User;
import io.github.ozkanpakdil.grepwise.security.JwtAuthenticationFilter;
import io.github.ozkanpakdil.grepwise.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Spring Test ContextCustomizer that excludes specific filters from the test application context
 * and provides mock implementations for tests.
 * 
 * This customizer:
 * 1. Removes the real JwtAuthenticationFilter and RateLimitingFilter beans
 * 2. Registers mock implementations of RateLimitingFilter, RateLimitingConfig, and TokenService
 * 
 * This allows tests to run without requiring the full security and rate limiting infrastructure.
 */
public class ExcludeFilterContextCustomizer implements ContextCustomizer {

    /**
     * Customizes the application context by excluding specific filters and registering mock implementations.
     * 
     * @param context The application context to customize
     * @param mergedConfig The merged context configuration
     */
    @Override
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
        // Register a BeanDefinitionRegistryPostProcessor that will exclude the filters and register mocks
        context.getBeanFactory().registerSingleton("excludeFilterPostProcessor", 
            (org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor) registry -> {
                // Remove JwtAuthenticationFilter if it exists
                if (registry.containsBeanDefinition("jwtAuthenticationFilter")) {
                    registry.removeBeanDefinition("jwtAuthenticationFilter");
                }
                
                // Remove RateLimitingFilter if it exists
                if (registry.containsBeanDefinition("rateLimitingFilter")) {
                    registry.removeBeanDefinition("rateLimitingFilter");
                }
                
                // Register mock RateLimitingConfig
                registerMockRateLimitingConfig(registry);
                
                // Register mock RateLimitingFilter
                registerMockRateLimitingFilter(registry);
                
                // Register mock TokenService
                registerMockTokenService(registry);
            });
    }
    
    /**
     * Registers a mock RateLimitingConfig bean.
     * 
     * @param registry The bean definition registry
     */
    private void registerMockRateLimitingConfig(BeanDefinitionRegistry registry) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(MockRateLimitingConfig.class);
        registry.registerBeanDefinition("rateLimitingConfig", beanDefinition);
    }
    
    /**
     * Registers a mock RateLimitingFilter bean.
     * 
     * @param registry The bean definition registry
     */
    private void registerMockRateLimitingFilter(BeanDefinitionRegistry registry) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(MockRateLimitingFilter.class);
        registry.registerBeanDefinition("rateLimitingFilter", beanDefinition);
    }
    
    /**
     * Registers a mock TokenService bean.
     * 
     * @param registry The bean definition registry
     */
    private void registerMockTokenService(BeanDefinitionRegistry registry) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition(MockTokenService.class);
        registry.registerBeanDefinition("tokenService", beanDefinition);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj != null && obj.getClass() == getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
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
     * Mock implementation of RateLimitingFilter for tests.
     * This implementation simply passes requests through without rate limiting.
     */
    public static class MockRateLimitingFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                throws ServletException, IOException {
            // Simply pass the request to the next filter in the chain
            filterChain.doFilter(request, response);
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