package com.africa.hr.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for CORS settings.
 * These properties are loaded from application.properties/yml with the prefix
 * 'spring.security.cors'.
 * 
 * Example configuration in application.yml:
 * spring:
 * security:
 * cors:
 * allowed-origins: http://localhost:3000,https://app.example.com
 * allowed-methods: GET,POST,PUT,DELETE,OPTIONS
 * allowed-headers: Authorization,Content-Type,X-Requested-With
 */
@Slf4j
@Data
@Validated
@Component
@ConfigurationProperties(prefix = "spring.security.cors")
public class CorsConfigProperties {

    /**
     * Comma-separated list of allowed origins (e.g.,
     * http://localhost:3000,https://app.example.com)
     */
    @NotBlank(message = "Allowed origins must not be blank")
    private String allowedOrigins;

    /**
     * Comma-separated list of allowed HTTP methods (e.g.,
     * GET,POST,PUT,DELETE,OPTIONS)
     */
    @NotBlank(message = "Allowed methods must not be blank")
    private String allowedMethods;

    /**
     * Comma-separated list of allowed headers (e.g.,
     * Authorization,Content-Type,X-Requested-With)
     */
    @NotBlank(message = "Allowed headers must not be blank")
    private String allowedHeaders;

    /**
     * Sets the allowed origins and logs the change.
     * 
     * @param allowedOrigins comma-separated list of allowed origins
     */
    public void setAllowedOrigins(String allowedOrigins) {
        log.debug("Setting allowed origins: {}", allowedOrigins);
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Sets the allowed methods and logs the change.
     * 
     * @param allowedMethods comma-separated list of allowed HTTP methods
     */
    public void setAllowedMethods(String allowedMethods) {
        log.debug("Setting allowed methods: {}", allowedMethods);
        this.allowedMethods = allowedMethods;
    }

    /**
     * Sets the allowed headers and logs the change.
     * 
     * @param allowedHeaders comma-separated list of allowed headers
     */
    public void setAllowedHeaders(String allowedHeaders) {
        log.debug("Setting allowed headers: {}", allowedHeaders);
        this.allowedHeaders = allowedHeaders;
    }
}
