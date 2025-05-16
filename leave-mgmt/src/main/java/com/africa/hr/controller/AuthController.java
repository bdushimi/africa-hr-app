package com.africa.hr.controller;

import com.africa.hr.dto.LoginRequest;
import com.africa.hr.dto.LoginResponse;
import com.africa.hr.dto.RegisterRequest;
import com.africa.hr.model.User;
import com.africa.hr.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Controller handling authentication-related endpoints.
 * Provides endpoints for user login, logout, and session management.
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String LOGIN_SUCCESS_MESSAGE = "Login successful for user: {}";
    private static final String LOGIN_FAILURE_MESSAGE = "Login failed for user: {}";
    private static final String LOGIN_ATTEMPT_MESSAGE = "Login attempt for user: {}";

    private final AuthService authService;

    /**
     * Authenticates a user and returns a JWT token upon successful login.
     *
     * @param loginRequest the login credentials
     * @return ResponseEntity containing the login response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.debug(LOGIN_ATTEMPT_MESSAGE, loginRequest.getEmail());
        try {
            LoginResponse response = authService.login(loginRequest);
            log.debug(LOGIN_SUCCESS_MESSAGE, loginRequest.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error(LOGIN_FAILURE_MESSAGE, loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    /**
     * Logs out the current user by invalidating their JWT token.
     *
     * @param authorizationHeader the Authorization header containing the JWT token
     * @return ResponseEntity with success message
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authorizationHeader) {
        try {
            authService.logout(authorizationHeader);
            return ResponseEntity.ok("Logged out successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Invalid token format");
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Logout failed");
        }
    }

    /**
     * Returns the current user's information after successful login.
     *
     * @return ResponseEntity containing the current user's information
     */
    @GetMapping("/login/success")
    public ResponseEntity<?> loginSuccess() {
        try {
            return ResponseEntity.ok(authService.getCurrentUser());
        } catch (UsernameNotFoundException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to get current user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Internal server error");
        }
    }

    /**
     * Handles login failures.
     *
     * @return ResponseEntity indicating login failure
     */
    @GetMapping("/login/failure")
    public ResponseEntity<Void> loginFailure() {
        log.debug("Login failure request received");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}