package com.africa.hr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class EnvTestController {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    @GetMapping("/env")
    public Map<String, Object> testEnv() {
        Map<String, Object> env = new HashMap<>();
        env.put("jwtSecret", jwtSecret != null ? "Set (length: " + jwtSecret.length() + ")" : "Not Set");
        env.put("jwtExpiration", jwtExpiration);
        return env;
    }
}