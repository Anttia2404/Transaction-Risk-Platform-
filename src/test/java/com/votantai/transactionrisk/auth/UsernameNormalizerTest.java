package com.votantai.transactionrisk.auth;

import org.junit.jupiter.api.Test;

import com.votantai.transactionrisk.auth.application.UsernameNormalizer;

import static org.assertj.core.api.Assertions.assertThat;

class UsernameNormalizerTest {

    @Test
    void trimsAndLowercasesWithLocaleIndependentRules() {
        assertThat(UsernameNormalizer.normalize("  ANALYST.User  ")).isEqualTo("analyst.user");
        assertThat(UsernameNormalizer.normalize(null)).isNull();
    }
}
