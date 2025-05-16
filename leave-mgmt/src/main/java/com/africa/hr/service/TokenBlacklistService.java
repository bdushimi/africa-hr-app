package com.africa.hr.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Service to manage blacklisted JWT tokens.
 */
@Slf4j
@Service
public class TokenBlacklistService {

    private final ConcurrentHashMap<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    /**
     * Adds a token to the blacklist with its expiration time.
     *
     * @param token          the JWT token to blacklist
     * @param expirationTime the token's expiration time in milliseconds
     */
    public void blacklistToken(String token, long expirationTime) {
        blacklistedTokens.put(token, expirationTime);
    }

    /**
     * Checks if a token is blacklisted.
     *
     * @param token the JWT token to check
     * @return true if the token is blacklisted, false otherwise
     */
    public boolean isBlacklisted(String token) {
        return blacklistedTokens.containsKey(token);
    }
}