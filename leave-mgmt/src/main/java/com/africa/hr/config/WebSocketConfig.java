package com.africa.hr.config;

import com.africa.hr.websocket.AuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration for WebSockets with STOMP messaging.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final AuthChannelInterceptor authChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple memory-based message broker to send messages to clients
        // Prefix for messages FROM server TO client
        registry.enableSimpleBroker("/topic", "/queue", "/user");

        // Prefix for messages FROM client TO server
        registry.setApplicationDestinationPrefixes("/app");

        // Enable user-specific destinations
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoints (connection entry points)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "http://localhost:8080") // In production, restrict to your domain
                .withSockJS(); // Enable SockJS fallback
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Add authentication interceptor to authenticate WebSocket connections
        registration.interceptors(authChannelInterceptor);
    }
}