package com.africa.hr.service;

import com.africa.hr.dto.LoginRequest;
import com.africa.hr.dto.LoginResponse;
import com.africa.hr.dto.RegisterRequest;
import com.africa.hr.model.Role;
import com.africa.hr.model.User;
import com.africa.hr.repository.RoleRepository;
import com.africa.hr.repository.UserRepository;
import com.africa.hr.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service handling authentication operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Authenticates a user and returns their information with a JWT token.
     *
     * @param loginRequest the login credentials
     * @return LoginResponse containing user information and JWT token
     */
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            log.debug("Attempting to authenticate user: {}", loginRequest.getEmail());

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

            User user = (User) authentication.getPrincipal();
            String token = jwtService.generateToken(user);
            log.debug("User {} authenticated successfully", loginRequest.getEmail());

            return LoginResponse.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .token(token)
                    .roles(Set.of(user.getRole().getName()))
                    .build();
        } catch (Exception e) {
            log.error("Authentication failed for user: {}", loginRequest.getEmail(), e);
            throw e;
        }
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            log.warn("Registration attempt with existing email: {}", registerRequest.getEmail());
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        // Assign default role
        Role userRole = roleRepository.findByName("ROLE_STAFF")
                .orElseThrow(() -> new RuntimeException("Default role not found"));
        user.setRole(userRole);

        userRepository.save(user);
        log.debug("New user registered: {}", registerRequest.getEmail());
    }

    /**
     * Logs out the current user by invalidating their JWT token.
     *
     * @param token the JWT token to invalidate
     */
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7); // Remove "Bearer " prefix
            jwtService.invalidateToken(token);
            SecurityContextHolder.clearContext();
            log.debug("User logged out successfully, token invalidated");
        } else {
            log.warn("Invalid token format for logout");
            throw new IllegalArgumentException("Invalid token format");
        }
    }

    /**
     * Returns the current user's information.
     *
     * @return LoginResponse containing the current user's information
     * @throws UsernameNotFoundException if no user is currently authenticated
     */
    public LoginResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("No authenticated user found");
        }

        User user = (User) authentication.getPrincipal();
        return LoginResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(Set.of(user.getRole().getName()))
                .build();
    }
}