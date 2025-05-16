package com.africa.hr.config;

import com.africa.hr.security.CustomUserDetailsService;
import com.africa.hr.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security configuration class that sets up:
 * - JWT-based authentication
 * - CORS configuration
 * - Password encoding
 * - Security filter chain
 * - Authentication provider
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String API_PATTERN = "/api/**";
    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String UNAUTHORIZED_MESSAGE = "Unauthorized: %s";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final int BCRYPT_STRENGTH = 10;

    private final CustomUserDetailsService userDetailsService;
    private final CorsConfigProperties corsConfigProperties;
    private final JwtAuthenticationFilter jwtAuthFilter;

    /**
     * Configures the security filter chain with JWT authentication, CORS, and
     * endpoint security.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(LOGIN_ENDPOINT).permitAll()
                        .anyRequest().authenticated())
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(String.format(UNAUTHORIZED_MESSAGE, authException.getMessage()));
                        }))
                .securityMatcher(API_PATTERN);

        log.debug("Security filter chain configured successfully");
        return http.build();
    }

    /**
     * Configures CORS settings based on application properties.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(splitAndTrim(corsConfigProperties.getAllowedOrigins()));
        configuration.setAllowedMethods(splitAndTrim(corsConfigProperties.getAllowedMethods()));
        configuration.setAllowedHeaders(splitAndTrim(corsConfigProperties.getAllowedHeaders()));
        configuration.setExposedHeaders(List.of(AUTHORIZATION_HEADER));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        log.debug("CORS configuration applied successfully");
        return source;
    }

    /**
     * Configures the authentication provider with user details service and password
     * encoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        log.debug("Authentication provider configured successfully");
        return authProvider;
    }

    /**
     * Configures the authentication manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configures the password encoder with BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }

    /**
     * Helper method to split and trim comma-separated values.
     */
    private List<String> splitAndTrim(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .toList();
    }
}