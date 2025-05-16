package com.africa.hr.websocket;

import com.africa.hr.model.User;
import com.africa.hr.repository.UserRepository;
import com.africa.hr.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Interceptor for WebSocket connections to authenticate users based on JWT
 * tokens.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final WebSocketSessionRegistry sessionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract authorization header
            List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");
            if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
                String authHeader = authorizationHeaders.get(0);
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String jwt = authHeader.substring(7);
                    String username = jwtService.extractUsername(jwt);

                    if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        Optional<User> userOpt = userRepository.findByEmail(username);

                        if (userOpt.isPresent()) {
                            User user = userOpt.get();
                            if (jwtService.isTokenValid(jwt, user)) {
                                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        user.getAuthorities());

                                // Set authentication in STOMP headers
                                accessor.setUser(authToken);

                                // Store session info
                                String sessionId = accessor.getSessionId();
                                if (sessionId != null) {
                                    sessionRegistry.registerSession(user.getId(), sessionId);
                                    log.info("User {} (ID: {}) connected with session ID: {}",
                                            user.getEmail(), user.getId(), sessionId);
                                }
                            }
                        }
                    }
                }
            }
        } else if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            // Handle disconnections
            String sessionId = accessor.getSessionId();
            if (sessionId != null) {
                sessionRegistry.removeSession(sessionId);
                log.info("Session disconnected: {}", sessionId);
            }
        }

        return message;
    }
}