package com.votantai.transactionrisk.auth.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.votantai.transactionrisk.auth.domain.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LocalUserSeedTest {

    @Test
    void seedIsOptInAndRestrictedToLocalProfile() {
        assertThat(LocalUserSeed.class.getAnnotation(Profile.class).value()).containsExactly("local");
        var condition = LocalUserSeed.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(condition.name()).containsExactly("app.auth.seed.enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
    }

    @Test
    void seedNormalizesUsernameAndHashesPasswordBeforePersistence() throws Exception {
        var repository = mock(UserRepository.class);
        var encoder = new BCryptPasswordEncoder();
        ApplicationRunner runner = new LocalUserSeed().seedLocalUser(repository, encoder,
                "  LOCAL.ADMIN  ", "local-password", "ADMIN");

        runner.run(null);

        verify(repository).insertIfAbsent(org.mockito.ArgumentMatchers.any(), eq("local.admin"),
                argThat(hash -> hash != null && encoder.matches("local-password", hash)), eq(Role.ADMIN));
    }
}
