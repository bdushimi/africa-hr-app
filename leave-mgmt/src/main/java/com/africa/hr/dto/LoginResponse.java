package com.africa.hr.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Data Transfer Object for login responses.
 * Contains the user's information and JWT token after successful
 * authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * The unique identifier of the user.
     */
    private Long id;

    /**
     * The user's email address.
     */
    private String email;

    /**
     * The user's first name.
     */
    private String firstName;

    /**
     * The user's last name.
     */
    private String lastName;

    /**
     * The JWT token for subsequent authenticated requests.
     */
    private String token;

    /**
     * The set of roles assigned to the user.
     */
    private Set<String> roles;
}