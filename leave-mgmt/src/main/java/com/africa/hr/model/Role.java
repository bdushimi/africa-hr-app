package com.africa.hr.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Role entity representing user roles in the system.
 * Roles are used for authorization and access control.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Role name is required")
    @Size(min = 3, max = 50, message = "Role name must be between 3 and 50 characters")
    @Column(nullable = false, unique = true)
    private String name;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    @Column(length = 255)
    private String description;

    /**
     * Checks if this role matches the given role name.
     *
     * @param roleName the role name to check against
     * @return true if the role names match, false otherwise
     */
    public boolean isRole(String roleName) {
        return this.name.equals(roleName);
    }

    /**
     * Checks if this role is an admin role.
     *
     * @return true if this is an admin role, false otherwise
     */
    public boolean isAdmin() {
        return this.name.equals("ROLE_ADMIN");
    }

    /**
     * Checks if this role is a manager role.
     *
     * @return true if this is a manager role, false otherwise
     */
    public boolean isManager() {
        return this.name.equals("ROLE_MANAGER");
    }
}