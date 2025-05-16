package com.africa.hr.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.africa.hr.service.TokenBlacklistService;

/**
 * Service for handling JWT token operations including generation, validation,
 * and claim extraction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String TOKEN_EMPTY_MESSAGE = "JWT token cannot be null or empty";
    private static final String TOKEN_EXPIRED_MESSAGE = "JWT token has expired";
    private static final String TOKEN_INVALID_MESSAGE = "Invalid JWT token";
    private static final String TOKEN_BLACKLISTED_MESSAGE = "JWT token has been invalidated";
    private static final String INVALID_SECRET_MESSAGE = "JWT secret key must be properly configured and at least 256 bits (32 bytes) long";

    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    @PostConstruct
    public void validateSecretKey() {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalStateException(INVALID_SECRET_MESSAGE);
        }
        try {
            byte[] keyBytes = java.util.Base64.getDecoder().decode(secretKey);
            if (keyBytes.length < 32) { // 256 bits = 32 bytes
                throw new IllegalStateException(INVALID_SECRET_MESSAGE);
            }
            log.info("JWT secret key validated successfully");
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(INVALID_SECRET_MESSAGE, e);
        }
    }

    /**
     * Invalidates a JWT token by adding it to the blacklist.
     *
     * @param token the JWT token to invalidate
     */
    public void invalidateToken(String token) {
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = extractAllClaims(token);
                long expirationTime = claims.getExpiration().getTime();
                tokenBlacklistService.blacklistToken(token, expirationTime);
                log.debug("Token invalidated successfully");
            } catch (Exception e) {
                log.error("Failed to invalidate token", e);
            }
        }
    }

    /**
     * Validates a JWT token against user details.
     *
     * @param token       the JWT token to validate
     * @param userDetails the user details to validate against
     * @return true if the token is valid, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (!StringUtils.hasText(token)) {
            log.error("Token is empty or null");
            return false;
        }

        try {
            // First verify the signature and parse the token
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Check if token is blacklisted
            if (tokenBlacklistService.isBlacklisted(token)) {
                log.error("Token is blacklisted");
                return false;
            }

            // Verify expiration
            if (claims.getExpiration().before(new Date())) {
                log.error("Token has expired");
                return false;
            }

            // Verify username matches
            String username = claims.getSubject();
            if (!username.equals(userDetails.getUsername())) {
                log.error("Token username does not match user details");
                return false;
            }

            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            log.error("JWT token has expired: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException | MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error validating JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the username from a JWT token.
     *
     * @param token the JWT token
     * @return the username (subject) from the token
     * @throws IllegalArgumentException if the token is invalid
     */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (Exception e) {
            log.error("Failed to extract username from token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token");
        }
    }

    /**
     * Extracts a specific claim from a JWT token.
     *
     * @param token          the JWT token
     * @param claimsResolver function to extract the desired claim
     * @return the extracted claim
     * @throws IllegalArgumentException if the token is invalid
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        validateToken(token);
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT token for a user.
     *
     * @param userDetails the user details
     * @return the generated JWT token
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT token for a user with additional claims.
     *
     * @param extraClaims additional claims to include in the token
     * @param userDetails the user details
     * @return the generated JWT token
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        String token = Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        log.debug("Generated JWT token for user: {}", userDetails.getUsername());
        return token;
    }

    private Date extractExpiration(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            log.error("Failed to extract expiration from token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token");
        }
    }

    // Remove redundant methods
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to extract claims from token: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid token");
        }
    }

    private Key getSigningKey() {
        try {
            if (secretKey == null || secretKey.isEmpty()) {
                throw new IllegalStateException(INVALID_SECRET_MESSAGE);
            }
            byte[] keyBytes = java.util.Base64.getDecoder().decode(secretKey);
            if (keyBytes.length < 32) {
                throw new IllegalStateException(INVALID_SECRET_MESSAGE);
            }
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (Exception e) {
            log.error("Failed to create signing key: {}", e.getMessage());
            throw new IllegalStateException(INVALID_SECRET_MESSAGE, e);
        }
    }

    private void validateToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException(TOKEN_EMPTY_MESSAGE);
        }
    }
}