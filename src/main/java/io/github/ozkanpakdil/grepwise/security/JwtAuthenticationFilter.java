package io.github.ozkanpakdil.grepwise.security;

import io.github.ozkanpakdil.grepwise.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filter for JWT authentication.
 * This filter extracts the JWT token from the request, validates it, and sets up the security context.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenService.validateToken(jwt)) {
                String userId = tokenService.getUserIdFromToken(jwt);
                String username = tokenService.getUsernameFromToken(jwt);
                List<String> roles = tokenService.getRolesFromToken(jwt);

                // Create authorities from roles and permissions
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                        .collect(Collectors.toList());

                // Add permissions as authorities
                // This assumes that permissions are stored in the token or can be derived from roles
                // In a real implementation, you might need to fetch permissions from a database
                // For now, we'll use a simple naming convention: role:permission
                roles.forEach(role -> {
                    if (role.equals("ADMIN")) {
                        // Admin has all permissions
                        authorities.add(new SimpleGrantedAuthority("user:view"));
                        authorities.add(new SimpleGrantedAuthority("user:create"));
                        authorities.add(new SimpleGrantedAuthority("user:edit"));
                        authorities.add(new SimpleGrantedAuthority("user:delete"));
                        authorities.add(new SimpleGrantedAuthority("role:view"));
                        authorities.add(new SimpleGrantedAuthority("role:create"));
                        authorities.add(new SimpleGrantedAuthority("role:edit"));
                        authorities.add(new SimpleGrantedAuthority("role:delete"));
                        authorities.add(new SimpleGrantedAuthority("log:view"));
                        authorities.add(new SimpleGrantedAuthority("log:search"));
                        authorities.add(new SimpleGrantedAuthority("log:export"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:view"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:create"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:edit"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:delete"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:share"));
                        authorities.add(new SimpleGrantedAuthority("alarm:view"));
                        authorities.add(new SimpleGrantedAuthority("alarm:create"));
                        authorities.add(new SimpleGrantedAuthority("alarm:edit"));
                        authorities.add(new SimpleGrantedAuthority("alarm:delete"));
                        authorities.add(new SimpleGrantedAuthority("alarm:acknowledge"));
                        authorities.add(new SimpleGrantedAuthority("settings:view"));
                        authorities.add(new SimpleGrantedAuthority("settings:edit"));
                    } else if (role.equals("USER")) {
                        // User has basic permissions
                        authorities.add(new SimpleGrantedAuthority("log:view"));
                        authorities.add(new SimpleGrantedAuthority("log:search"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:view"));
                        authorities.add(new SimpleGrantedAuthority("alarm:view"));
                        authorities.add(new SimpleGrantedAuthority("alarm:acknowledge"));
                    } else if (role.equals("MANAGER")) {
                        // Manager has elevated permissions
                        authorities.add(new SimpleGrantedAuthority("log:view"));
                        authorities.add(new SimpleGrantedAuthority("log:search"));
                        authorities.add(new SimpleGrantedAuthority("log:export"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:view"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:create"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:edit"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:delete"));
                        authorities.add(new SimpleGrantedAuthority("dashboard:share"));
                        authorities.add(new SimpleGrantedAuthority("alarm:view"));
                        authorities.add(new SimpleGrantedAuthority("alarm:create"));
                        authorities.add(new SimpleGrantedAuthority("alarm:edit"));
                        authorities.add(new SimpleGrantedAuthority("alarm:delete"));
                        authorities.add(new SimpleGrantedAuthority("alarm:acknowledge"));
                        authorities.add(new SimpleGrantedAuthority("user:view"));
                    }
                });

                // Use username as principal to be stable across restarts; keep userId in details for reference
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username != null ? username : userId, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        // Fallback for transports that cannot set headers (e.g., EventSource/SSE): allow access_token query param
        String tokenParam = request.getParameter("access_token");
        if (StringUtils.hasText(tokenParam)) {
            return tokenParam;
        }
        return null;
    }
}