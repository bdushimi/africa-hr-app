package com.africa.hr.security;

import com.africa.hr.model.User;
import com.africa.hr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Custom implementation of Spring Security's UserDetailsService.
 * Loads user details from the database using the email address.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private static final String USER_NOT_FOUND_MESSAGE = "User not found with email: %s";
    private static final String INVALID_EMAIL_MESSAGE = "Email cannot be null or empty";

    private final UserRepository userRepository;

    /**
     * Loads a user by their email address.
     *
     * @param email the email address of the user to load
     * @return the UserDetails object containing the user's information
     * @throws UsernameNotFoundException if the user is not found
     * @throws IllegalArgumentException  if the email is null or empty
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (!StringUtils.hasText(email)) {
            log.error(INVALID_EMAIL_MESSAGE);
            throw new IllegalArgumentException(INVALID_EMAIL_MESSAGE);
        }

        log.debug("Loading user by email: {}", email);
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> {
                        String errorMessage = String.format(USER_NOT_FOUND_MESSAGE, email);
                        log.error(errorMessage);
                        return new UsernameNotFoundException(errorMessage);
                    });

            log.debug("User {} loaded successfully with roles: {}", email, user.getRole());
            return user;
        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error loading user by email: {}", email, e);
            throw new UsernameNotFoundException("Error loading user", e);
        }
    }
}