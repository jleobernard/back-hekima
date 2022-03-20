package com.leo.hekima.configuration;

import com.leo.hekima.exception.UnrecoverableServiceException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

@Configuration
public class JwtConfiguration {
    public static class JwtProperties {
        private final String secretKey;
        private final int validityInMs;

        public JwtProperties(String secretKey, int validityInMs) {
            this.secretKey = secretKey;
            this.validityInMs = validityInMs;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public int getValidityInMs() {
            return validityInMs;
        }
    }

    @Bean
    public JwtProperties jwtProperties() {
        final Map<String, String> env = System.getenv();
        return new JwtProperties(
            Optional.ofNullable(env.get("NOTES_JWT_SECRET_KEY"))
                .orElseThrow(() -> new UnrecoverableServiceException("Set a value for NOTES_JWT_SECRET_KEY")),
            Integer.parseInt(env.getOrDefault("NOTES_JWT_VALIDITY_MS", "3600000"))
        );
    }
}
