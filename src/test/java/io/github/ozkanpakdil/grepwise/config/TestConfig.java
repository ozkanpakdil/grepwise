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
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.DefaultLdapAuthoritiesPopulator;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test configuration that provides mock implementations of beans for tests. This configuration is used to replace real
 * implementations with test doubles to avoid dependencies on external systems and to simplify test setup.
 */
@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder,
            UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(passwordEncoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        UserDetails testUser = org.springframework.security.core.userdetails.User.builder()
                .username("test")
                .password(passwordEncoder.encode("test"))
                .roles("USER")
                .build();
        manager.createUser(testUser);
        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public LdapConfig ldapConfig() {
        return new MockLdapConfig();
    }

    @Bean
    public LdapAuthenticationProvider ldapAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        // This is a simple mock that delegates to our standard authentication provider
        return new LdapAuthenticationProvider(
                new BindAuthenticator(new DefaultSpringSecurityContextSource("ldap://localhost:389/dc=example,dc=com")),
                new DefaultLdapAuthoritiesPopulator(
                        new DefaultSpringSecurityContextSource("ldap://localhost:389/dc=example,dc=com"), "ou=groups"));
    }

    /**
     * Provides a mock implementation of RateLimitingFilter for tests. This implementation simply passes requests through
     * without rate limiting.
     *
     * @return A mock RateLimitingFilter
     */
    @Bean
    @Primary
    public RateLimitingFilter testRateLimitingFilter() {
        return new MockRateLimitingFilter();
    }

    /**
     * Provides a mock implementation of RateLimitingConfig for tests. This implementation provides dummy buckets that always
     * allow requests.
     *
     * @return A mock RateLimitingConfig
     */
    @Bean
    @Primary
    public RateLimitingConfig testRateLimitingConfig() {
        return new MockRateLimitingConfig();
    }

    /**
     * Provides a mock implementation of TokenService for tests.
     *
     * @return A mock TokenService
     */
    @Bean
    @Primary
    public TokenService tokenService() {
        // Create a mock TokenService
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

    /**
     * Mock implementation of RateLimitingFilter for tests. This implementation simply passes requests through without rate
     * limiting.
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
     * Mock implementation of RateLimitingConfig for tests. This implementation provides dummy buckets that always allow
     * requests.
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
     * Mock implementation of LdapConfig for tests.
     */
    public static class MockLdapConfig extends LdapConfig {
        @Override
        public boolean isLdapEnabled() {
            return false;  // Disable LDAP for tests
        }
    }
}