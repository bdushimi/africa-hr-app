package com.africa.hr.service;

import com.africa.hr.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    /**
     * Find a user by their ID.
     *
     * @param id the user ID
     * @return optional containing the user if found
     */
    Optional<User> findById(Long id);

    /**
     * Find a user by their username.
     *
     * @param username the username
     * @return optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    List<User> findAllEmployees();
}