package com.africa.hr.config;

import org.springframework.context.annotation.Configuration;
import
org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
import
org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;

/**
* Security configuration for WebSocket connections.
*/
@Configuration
public class WebSocketSecurityConfig extends
AbstractSecurityWebSocketMessageBrokerConfigurer {

/**
* Configures security rules for WebSocket messages.
*/
@Override
protected void configureInbound(MessageSecurityMetadataSourceRegistry
messages) {
messages
.nullDestMatcher().authenticated()
.simpDestMatchers("/app/**").authenticated()
.simpSubscribeDestMatchers("/user/**").authenticated()
.simpSubscribeDestMatchers("/queue/**").authenticated()
.simpSubscribeDestMatchers("/topic/role/ROLE_MANAGER").hasRole("MANAGER")
.simpSubscribeDestMatchers("/topic/role/ROLE_ADMIN").hasRole("ADMIN")
.simpSubscribeDestMatchers("/topic/global").authenticated()
.anyMessage().denyAll();
}

/**
* Disables CSRF protection for WebSocket connections.
*/
@Override
protected boolean sameOriginDisabled() {
return true;
}
}