package io.github.ozkanpakdil.grepwise.config;

import io.github.ozkanpakdil.grepwise.filter.RateLimitingFilter;
import io.github.ozkanpakdil.grepwise.security.JwtAuthenticationFilter;
import io.github.ozkanpakdil.grepwise.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.core.annotation.Order;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Security configuration for the application.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    private final TokenService tokenService;

    private final LdapConfig ldapConfig;

    private final LdapAuthenticationProvider ldapAuthenticationProvider;

    public WebSecurityConfig(TokenService tokenService,
                             LdapConfig ldapConfig,
                             @Autowired(required = false) LdapAuthenticationProvider ldapAuthenticationProvider) {
        this.tokenService = tokenService;
        this.ldapConfig = ldapConfig;
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
    }

    /**
     * Creates an authentication manager that includes the LDAP authentication provider if LDAP is enabled.
     * This bean is only created if LDAP is enabled and properly configured.
     *
     * @return The authentication manager
     */
    @Bean
    @ConditionalOnProperty(name = "grepwise.ldap.enabled", havingValue = "true")
    public AuthenticationManager authenticationManager() {
        try {
            if (ldapAuthenticationProvider != null) {
                logger.info("Creating LDAP authentication manager");
                return new ProviderManager(Collections.singletonList(ldapAuthenticationProvider));
            } else {
                logger.warn("LDAP authentication provider is null, cannot create authentication manager");
                throw new IllegalStateException("LDAP authentication provider is required when LDAP is enabled");
            }
        } catch (Exception e) {
            logger.error("Failed to create LDAP authentication manager", e);
            throw e;
        }
    }

    /**
     * Configure security for the application.
     *
     * Defines beans for RateLimitingFilter and two security chains (API and default).
     */
    /*@Bean
    public RateLimitingFilter rateLimitingFilter(RateLimitingConfig rateLimitingConfig) {
        return new RateLimitingFilter(rateLimitingConfig);
    }*/

    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        // Limit this chain to API paths only
        http.securityMatcher("/api/**");

        // Set the authentication manager if LDAP is enabled
        if (ldapConfig != null && ldapConfig.isLdapEnabled()) {
            try {
                AuthenticationManager authManager = authenticationManager();
                http.authenticationManager(authManager);
                logger.info("LDAP authentication manager configured");
            } catch (Exception e) {
                logger.warn("Failed to configure LDAP authentication manager, continuing with default authentication", e);
            }
        }

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> res.sendError(HttpStatus.UNAUTHORIZED.value()))
                        .accessDeniedHandler((req, res, e) -> res.sendError(HttpStatus.FORBIDDEN.value()))
                )
                // Add rate limiting filter before authentication filter
                .addFilterBefore(new JwtAuthenticationFilter(tokenService), UsernamePasswordAuthenticationFilter.class)
//                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        // Add LDAP login endpoint if LDAP is enabled
                        .requestMatchers(ldapConfig != null && ldapConfig.isLdapEnabled() ?
                                "/api/auth/ldap/login" : "/api/auth/non-existent-path").permitAll()
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/search**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/search/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/count").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/fields").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/sources").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/levels").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/time-aggregation").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/histogram").permitAll()

                        // User management endpoints - require admin role
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ROLE_ADMIN")
                        // Profile should be accessible to any authenticated user
                        .requestMatchers(HttpMethod.GET, "/api/profile").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/users").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/users/role/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/users/permission/**").hasAuthority("ROLE_ADMIN")

                        // Role management endpoints - require admin role
                        .requestMatchers("/api/roles/**").hasAuthority("ROLE_ADMIN")

                        // Dashboard endpoints - GET public, mutations protected
                        .requestMatchers(HttpMethod.GET, "/api/dashboards**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/dashboards").hasAuthority("dashboard:create")
                        .requestMatchers(HttpMethod.PUT, "/api/dashboards/**").hasAuthority("dashboard:edit")
                        .requestMatchers(HttpMethod.DELETE, "/api/dashboards/**").hasAuthority("dashboard:delete")
                        .requestMatchers("/api/dashboards/*/share").hasAuthority("dashboard:share")

                        // Alarm endpoints - require appropriate permissions
                        .requestMatchers(HttpMethod.GET, "/api/alarms").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/alarms").hasAuthority("alarm:create")
                        .requestMatchers(HttpMethod.PUT, "/api/alarms/**").hasAuthority("alarm:edit")
                        .requestMatchers(HttpMethod.DELETE, "/api/alarms/**").hasAuthority("alarm:delete")
                        .requestMatchers("/api/alarms/*/acknowledge").hasAuthority("alarm:acknowledge")

                        // Allow CORS preflight for all API endpoints
                        .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()

                        // Configuration endpoints - allow for now (development/setup)
                        .requestMatchers("/api/config/**").permitAll()

                        // Require authentication for all other API endpoints
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/**");

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> res.sendError(HttpStatus.UNAUTHORIZED.value()))
                        .accessDeniedHandler((req, res, e) -> res.sendError(HttpStatus.FORBIDDEN.value()))
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/", "/error", "/index.html", "/favicon.ico", "/manifest.webmanifest",
                                "/assets/**", "/static/**", "/vite.svg",
                                "/*.css", "/*.js", "/*.map", "/*.png", "/*.jpg", "/*.jpeg", "/*.svg", "/*.gif"
                        ).permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * Disable servlet container auto-registration for RateLimitingFilter since it is added to the Spring Security filter chain.
     * This avoids duplicate registration that can cause recursive filter invocation and StackOverflowError.
     */
    /*@Bean
    public FilterRegistrationBean<RateLimitingFilter> disableRateLimitingFilterAutoRegistration(RateLimitingFilter filter) {
        FilterRegistrationBean<RateLimitingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }*/

    /**
     * Configure CORS for the application.
     *
     * @return The CORS configuration
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Cache-Control", "Pragma"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}