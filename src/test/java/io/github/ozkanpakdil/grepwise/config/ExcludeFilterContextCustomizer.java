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
import org.mockito.Mockito;
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
        // First, remove the existing bean definition if it exists
        if (registry.containsBeanDefinition("tokenService")) {
            registry.removeBeanDefinition("tokenService");
        }

        // Create a RootBeanDefinition for TokenService
        RootBeanDefinition beanDefinition = new RootBeanDefinition(TokenService.class);
        
        // Set a factory method to create and configure the mock
        beanDefinition.setFactoryMethodName("createMockTokenService");
        beanDefinition.setFactoryBeanName("tokenServiceFactory");
        
        // Register the factory bean that will create our mock
        RootBeanDefinition factoryBeanDefinition = new RootBeanDefinition(TokenServiceFactory.class);
        registry.registerBeanDefinition("tokenServiceFactory", factoryBeanDefinition);
        
        // Add @Primary annotation to ensure this bean is preferred over any other implementations
        beanDefinition.setPrimary(true);
        registry.registerBeanDefinition("tokenService", beanDefinition);
    }
    
    /**
     * Factory class for creating mock TokenService instances.
     */
    public static class TokenServiceFactory {
        /**
         * Creates and configures a mock TokenService.
         * 
         * @return A configured mock TokenService
         */
        public TokenService createMockTokenService() {
            TokenService mockTokenService = Mockito.mock(TokenService.class);
            
            // Configure the mock to return dummy values
            Mockito.when(mockTokenService.generateToken(Mockito.any(User.class))).thenReturn("dummy.jwt.token");
            Mockito.when(mockTokenService.generateRefreshToken(Mockito.any(User.class))).thenReturn("dummy.jwt.token");
            Mockito.when(mockTokenService.validateToken(Mockito.anyString())).thenReturn(true);
            Mockito.when(mockTokenService.getUserIdFromToken(Mockito.anyString())).thenReturn("test-user-id");
            Mockito.when(mockTokenService.getUsernameFromToken(Mockito.anyString())).thenReturn("test-user");
            
            List<String> roles = new ArrayList<>();
            roles.add("ROLE_USER");
            Mockito.when(mockTokenService.getRolesFromToken(Mockito.anyString())).thenReturn(roles);
            
            Date expirationDate = new Date(System.currentTimeMillis() + 3600000); // 1 hour from now
            Mockito.when(mockTokenService.getExpirationDateFromToken(Mockito.anyString())).thenReturn(expirationDate);
            Mockito.when(mockTokenService.isTokenExpired(Mockito.anyString())).thenReturn(false);
            
            return mockTokenService;
        }
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
    
}