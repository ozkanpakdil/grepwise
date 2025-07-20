package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for handling JWT token generation, validation, and refresh.
 */
@Service
public class TokenService {

    @Value("${jwt.secret:defaultSecretKey}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private long expiration;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days in milliseconds
    private long refreshExpiration;

    /**
     * Generate a JWT token for a user.
     *
     * @param user The user to generate a token for
     * @return The generated token
     */
    public String generateToken(User user) {
        return generateToken(user, expiration);
    }

    /**
     * Generate a refresh token for a user.
     *
     * @param user The user to generate a refresh token for
     * @return The generated refresh token
     */
    public String generateRefreshToken(User user) {
        return generateToken(user, refreshExpiration);
    }

    /**
     * Generate a token with custom claims and expiration.
     *
     * @param user       The user to generate a token for
     * @param expiration The expiration time in milliseconds
     * @return The generated token
     */
    private String generateToken(User user, long expiration) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId());
        claims.put("username", user.getUsername());
        claims.put("roles", user.getRoleNames());
        
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate a token.
     *
     * @param token The token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the user ID from a token.
     *
     * @param token The token to get the user ID from
     * @return The user ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * Get the username from a token.
     *
     * @param token The token to get the username from
     * @return The username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (String) claims.get("username");
    }

    /**
     * Get the roles from a token.
     *
     * @param token The token to get the roles from
     * @return The roles
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return (List<String>) claims.get("roles");
    }

    /**
     * Get the expiration date from a token.
     *
     * @param token The token to get the expiration date from
     * @return The expiration date
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }

    /**
     * Check if a token is expired.
     *
     * @param token The token to check
     * @return true if the token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * Get all claims from a token.
     *
     * @param token The token to get the claims from
     * @return The claims
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Get the signing key for JWT.
     *
     * @return The signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}