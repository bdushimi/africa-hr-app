package com.africa.hr.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class EnvConfig implements EnvironmentPostProcessor {
    private static final String ENV_FILE = ".env";
    private static final String ENV_PROPERTY_SOURCE = "envFile";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            Map<String, Object> envVars = new HashMap<>();

            // First try to load from the project root directory
            Path envPath = Paths.get(ENV_FILE);
            if (Files.exists(envPath)) {
                loadEnvVars(Files.newBufferedReader(envPath), envVars);
            } else {
                // If not found in root, try to load from classpath
                Resource resource = new ClassPathResource(ENV_FILE);
                if (resource.exists()) {
                    loadEnvVars(new BufferedReader(new InputStreamReader(resource.getInputStream())), envVars);
                } else {
                    System.err.println("Warning: .env file not found in project root or classpath");
                    return;
                }
            }

            // Add the environment variables as a property source
            environment.getPropertySources().addFirst(new MapPropertySource(ENV_PROPERTY_SOURCE, envVars));
            System.out.println("Successfully loaded environment variables from .env file");
        } catch (IOException e) {
            System.err.println("Error loading .env file: " + e.getMessage());
        }
    }

    private void loadEnvVars(BufferedReader reader, Map<String, Object> envVars) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            int separatorIndex = line.indexOf('=');
            if (separatorIndex > 0) {
                String key = line.substring(0, separatorIndex).trim();
                String value = line.substring(separatorIndex + 1).trim();

                // Remove quotes if present
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }

                // Remove comments
                int commentIndex = value.indexOf('#');
                if (commentIndex > 0) {
                    value = value.substring(0, commentIndex).trim();
                }

                if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
                    envVars.put(key, value);
                }
            }
        }
    }
}