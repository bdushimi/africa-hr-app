package com.africa.hr.service;

import com.africa.hr.model.User;
import com.africa.hr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        log.debug("Finding user by ID: {}", id);
        return userRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username (email): {}", username);
        return userRepository.findByEmail(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAllEmployees() {
        log.debug("Finding all employees");
        return userRepository.findAllEmployees();
    }
}