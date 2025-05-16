package com.africa.hr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import com.africa.hr.config.CorsConfigProperties;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.africa.hr.repository")
@EnableConfigurationProperties(CorsConfigProperties.class)
public class LeaveMgmtApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveMgmtApplication.class, args);
    }

}