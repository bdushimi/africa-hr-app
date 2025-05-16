package com.africa.hr.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for tracking WebSocket sessions and their associated user IDs.
 * This allows the application to know which users are currently connected.
 */
@Component
@Slf4j
public class WebSocketSessionRegistry {

    // Map of user IDs to their session IDs (one user can have multiple sessions,
    // e.g., multiple browser tabs)
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    // Inverse map to quickly find user ID by session ID
    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();

    /**
     * Register a new WebSocket session for a user.
     *
     * @param userId    the user ID
     * @param sessionId the WebSocket session ID
     */
    public void registerSession(Long userId, String sessionId) {
        // Add to user -> sessions map
        userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // Add to session -> user map
        sessionUsers.put(sessionId, userId);

        log.debug("Registered session: {} for user: {}", sessionId, userId);
    }

    /**
     * Remove a WebSocket session when a user disconnects.
     *
     * @param sessionId the WebSocket session ID to remove
     */
    public void removeSession(String sessionId) {
        Long userId = sessionUsers.remove(sessionId);

        if (userId != null) {
            Set<String> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(sessionId);

                // If this user has no more sessions, remove the user entry
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }

            log.debug("Removed session: {} for user: {}", sessionId, userId);
        }
    }

    /**
     * Check if a user is currently connected.
     *
     * @param userId the user ID to check
     * @return true if the user has at least one active session
     */
    public boolean isUserConnected(Long userId) {
        Set<String> sessions = userSessions.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * Get all session IDs for a specific user.
     *
     * @param userId the user ID
     * @return a set of session IDs for the user
     */
    public Set<String> getSessionsForUser(Long userId) {
        return userSessions.getOrDefault(userId, ConcurrentHashMap.newKeySet());
    }

    /**
     * Get all connected user IDs.
     *
     * @return a set of all user IDs with active connections
     */
    public Set<Long> getConnectedUsers() {
        return userSessions.keySet().stream()
                .filter(userId -> !userSessions.get(userId).isEmpty())
                .collect(Collectors.toSet());
    }
}