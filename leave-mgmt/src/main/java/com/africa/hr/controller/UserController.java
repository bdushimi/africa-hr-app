package com.africa.hr.controller;

import com.africa.hr.model.User;
import com.africa.hr.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users")
public class UserController {

    private final UserService userService;

    /**
     * Get a user by their email address.
     *
     * @param email the email address of the user
     * @return the user if found
     */
    @GetMapping("/by-email/{email}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Get user by email", description = "Retrieves a user by their email address. "
            + "Only administrators and managers can access this endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found"),
            @ApiResponse(responseCode = "400", description = "Invalid email format"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - User is not authenticated"),
            @ApiResponse(responseCode = "403", description = "Forbidden - User does not have required role"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<User> getUserByEmail(
            @Parameter(description = "Email address of the user") @PathVariable @NotNull @Email String email) {
        log.info("Getting user by email: {}", email);

        User user = userService.findByUsername(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        return ResponseEntity.ok(user);
    }
}