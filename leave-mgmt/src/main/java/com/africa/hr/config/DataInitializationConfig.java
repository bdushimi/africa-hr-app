package com.africa.hr.config;

import com.africa.hr.model.Role;
import com.africa.hr.model.User;
import com.africa.hr.repository.RoleRepository;
import com.africa.hr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializationConfig {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void verifyDataInitialization() {
        log.info("Verifying data initialization...");

        boolean hasBasicRoles = verifyBasicRoles();
        boolean hasAdminUser = verifyAdminUser();
        boolean hasManagerUser = verifyManagerUser();
        boolean hasDefaultUser = verifyDefaultUser();

        // Reset admin password to ensure it's correct
        resetAdminPassword();

        if (hasBasicRoles && hasAdminUser && hasManagerUser && hasDefaultUser) {
            log.info("\u001B[32m✓ Data initialization verified successfully\u001B[0m");
        } else {
            log.warn(
                    "\u001B[33m⚠ Data initialization verification failed. Please check the logs above for details.\u001B[0m");
        }
    }

    private boolean verifyBasicRoles() {
        boolean hasUserRole = roleRepository.findByName("ROLE_STAFF").isPresent();
        boolean hasAdminRole = roleRepository.findByName("ROLE_ADMIN").isPresent();
        boolean hasManagerRole = roleRepository.findByName("ROLE_MANAGER").isPresent();

        if (!hasUserRole) {
            log.warn("⚠ ROLE_STAFF role is missing");
        }
        if (!hasAdminRole) {
            log.warn("⚠ ROLE_ADMIN role is missing");
        }
        if (!hasManagerRole) {
            log.warn("⚠ ROLE_MANAGER role is missing");
        }

        return hasUserRole && hasAdminRole && hasManagerRole;
    }

    private boolean verifyAdminUser() {
        boolean hasAdminUser = userRepository.existsByEmail("africahrapp+hradmin@gmail.com");
        if (!hasAdminUser) {
            log.warn("⚠ Admin user is missing");
        }
        return hasAdminUser;
    }

    private boolean verifyManagerUser() {
        boolean hasManagerUser = userRepository.existsByEmail("africahrapp+opsmanager@gmail.com");
        if (!hasManagerUser) {
            log.warn("⚠ Manager user is missing");
        }
        return hasManagerUser;
    }

    private boolean verifyDefaultUser() {
        boolean hasDefaultUser = userRepository.existsByEmail("africahrapp+defaultuser@gmail.com");
        if (!hasDefaultUser) {
            log.warn("⚠ Default user is missing");
        }
        return hasDefaultUser;
    }

    /**
     * Utility method to create a role if it doesn't exist
     */
    @Transactional
    public Role createRoleIfNotExists(String name, String description) {
        String roleName = name.startsWith("ROLE_") ? name : "ROLE_" + name;
        return roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(roleName);
                    role.setDescription(description);
                    return roleRepository.save(role);
                });
    }

    /**
     * Utility method to create an admin user if it doesn't exist
     */
    @Transactional
    public void createAdminUserIfNotExists(Role adminRole) {
        if (!userRepository.existsByEmail("africahrapp+hradmin@gmail.com")) {
            User admin = new User();
            admin.setFirstName("Admin");
            admin.setLastName("HR");
            admin.setEmail("africahrapp+hradmin@gmail.com");
            admin.setPassword(passwordEncoder.encode("Enter@123"));
            admin.setStatus(User.Status.ACTIVE);
            admin.setJoinedDate(LocalDate.now());

            admin.setRole(adminRole);

            userRepository.save(admin);
            log.info("Admin user created successfully");
        }
    }

    /**
     * Utility method to create a default user if it doesn't exist
     */
    @Transactional
    public void createDefaultUserIfNotExists(Role userRole) {
        if (!userRepository.existsByEmail("africahrapp+defaultuser@gmail.com")) {
            User defaultUser = new User();
            defaultUser.setFirstName("Default");
            defaultUser.setLastName("User");
            defaultUser.setEmail("africahrapp+defaultuser@gmail.com");
            defaultUser.setPassword(passwordEncoder.encode("Enter@123"));
            defaultUser.setStatus(User.Status.ACTIVE);
            defaultUser.setJoinedDate(LocalDate.now());

            defaultUser.setRole(userRole);

            userRepository.save(defaultUser);
            log.info("Default user created successfully");
        }
    }

    /**
     * Utility method to create a manager user if it doesn't exist
     */
    @Transactional
    public void createDefaultManagerIfNotExists(Role managerRole) {
        if (!userRepository.existsByEmail("africahrapp+opsmanager@gmail.com")) {
            User manager = new User();
            manager.setFirstName("Manager");
            manager.setLastName("OPs");
            manager.setEmail("africahrapp+opsmanager@gmail.com");
            manager.setPassword(passwordEncoder.encode("Enter@123"));
            manager.setStatus(User.Status.ACTIVE);
            manager.setJoinedDate(LocalDate.now());

            manager.setRole(managerRole);

            userRepository.save(manager);
            log.info("Default manager user created successfully");
        }
    }

    /**
     * Utility method to reset admin password
     */
    @Transactional
    public void resetAdminPassword() {
        userRepository.findByEmail("africahrapp+hradmin@gmail.com")
                .ifPresent(admin -> {
                    admin.setPassword(passwordEncoder.encode("Enter@123"));
                    userRepository.save(admin);
                    log.info("Admin password has been reset");
                });
    }
}