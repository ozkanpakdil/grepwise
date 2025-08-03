package io.github.ozkanpakdil.grepwise.config;

import io.github.ozkanpakdil.grepwise.filter.RateLimitingFilter;
import io.github.ozkanpakdil.grepwise.security.JwtAuthenticationFilter;
import io.github.ozkanpakdil.grepwise.service.TokenService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
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

    private final TokenService tokenService;

    private final RateLimitingFilter rateLimitingFilter;

    private final LdapConfig ldapConfig;

    private final LdapAuthenticationProvider ldapAuthenticationProvider;

    public WebSecurityConfig(TokenService tokenService, RateLimitingFilter rateLimitingFilter, LdapConfig ldapConfig,
                             LdapAuthenticationProvider ldapAuthenticationProvider) {
        this.tokenService = tokenService;
        this.rateLimitingFilter = rateLimitingFilter;
        this.ldapConfig = ldapConfig;
        this.ldapAuthenticationProvider = ldapAuthenticationProvider;
    }

    /**
     * Creates an authentication manager that includes the LDAP authentication provider if LDAP is enabled.
     * This bean is only created if the security.enabled property is true (default) and
     * the auth.manager.enabled property is true (default).
     *
     * @return The authentication manager
     */
    @Bean
    @ConditionalOnProperty(name = {"security.enabled", "auth.manager.enabled"}, havingValue = "true")
    public AuthenticationManager authenticationManager() {
        if (ldapConfig != null && ldapConfig.isLdapEnabled() && ldapAuthenticationProvider != null) {
            return new ProviderManager(Collections.singletonList(ldapAuthenticationProvider));
        }
        // Return an empty ProviderManager if LDAP is not enabled
        // The JWT filter will handle authentication in this case
        return new ProviderManager(Collections.emptyList());
    }

    /**
     * Configure security for the application.
     *
     * @param http The HttpSecurity to configure
     * @return The configured SecurityFilterChain
     * @throws Exception If an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Set the authentication manager if LDAP is enabled
        if (ldapConfig != null && ldapConfig.isLdapEnabled()) {
            http.authenticationManager(authenticationManager());
        }

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Add rate limiting filter before authentication filter
                .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new JwtAuthenticationFilter(tokenService), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        // Add LDAP login endpoint if LDAP is enabled
                        .requestMatchers(ldapConfig != null && ldapConfig.isLdapEnabled() ?
                                "/api/auth/ldap/login" : "/api/auth/non-existent-path").permitAll()
                        // Public endpoints
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/count").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/fields").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/sources").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/levels").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/time-aggregation").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/logs/histogram").permitAll()

                        // User management endpoints - require admin role
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/users").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/users/role/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/users/permission/**").hasAuthority("ROLE_ADMIN")

                        // Role management endpoints - require admin role
                        .requestMatchers("/api/roles/**").hasAuthority("ROLE_ADMIN")

                        // Dashboard endpoints - require appropriate permissions
                        .requestMatchers(HttpMethod.GET, "/api/dashboards").hasAnyAuthority("ROLE_USER", "ROLE_ADMIN")
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

                        // Settings endpoints - require admin role
                        .requestMatchers("/api/settings/**").hasAuthority("ROLE_ADMIN")

                        // Require authentication for all other endpoints
                        .anyRequest().authenticated()
                );

        return http.build();
    }

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
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}