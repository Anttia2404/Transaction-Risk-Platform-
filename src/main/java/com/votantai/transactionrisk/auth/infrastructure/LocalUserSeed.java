package com.votantai.transactionrisk.auth.infrastructure;

import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.votantai.transactionrisk.auth.application.UsernameNormalizer;
import com.votantai.transactionrisk.auth.domain.Role;

@Configuration
@Profile("local")
@ConditionalOnProperty(name = "app.auth.seed.enabled", havingValue = "true")
public class LocalUserSeed {

    @Bean
    ApplicationRunner seedLocalUser(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.auth.seed.username:}") String seedUsername,
            @Value("${app.auth.seed.password:}") String seedPassword,
            @Value("${app.auth.seed.role:ADMIN}") String seedRole) {
        return args -> {
            String username = UsernameNormalizer.normalize(seedUsername);
            if (username == null || username.isBlank() || seedPassword == null || seedPassword.isBlank()) {
                throw new IllegalStateException("Local user seed requires a non-empty username and password");
            }
            if (seedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException("Local user seed password must not exceed 72 UTF-8 bytes for BCrypt");
            }
            if (username.length() > 100) {
                throw new IllegalStateException("Local user seed username exceeds 100 characters");
            }
            Role role;
            try {
                role = Role.valueOf(seedRole);
            } catch (RuntimeException exception) {
                throw new IllegalStateException("Local user seed role must be ANALYST or ADMIN");
            }
            userRepository.insertIfAbsent(UUID.randomUUID(), username,
                    passwordEncoder.encode(seedPassword), role);
        };
    }
}
