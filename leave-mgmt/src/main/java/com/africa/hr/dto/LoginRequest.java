package com.africa.hr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Data Transfer Object for login requests.
 * Contains the user's email and password for authentication.
 */
@Data
public class LoginRequest {

    /**
     * The user's email address.
     * Must be a valid email format and cannot be blank.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    /**
     * The user's password.
     * Must not be blank and should meet the application's password requirements.
     */
    @NotBlank(message = "Password is required")
    private String password;
}