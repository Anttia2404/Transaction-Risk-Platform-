package com.votantai.transactionrisk;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@TestConfiguration
@Import(SecurityProbeConfiguration.ProbeController.class)
public class SecurityProbeConfiguration {

    @Bean
    @Order(0)
    SecurityFilterChain testProbeFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            AuthenticationEntryPoint problemAuthenticationEntryPoint,
            AccessDeniedHandler problemAccessDeniedHandler) throws Exception {
        return http.securityMatcher("/__test__/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/__test__/analyst").hasRole("ANALYST")
                        .requestMatchers("/__test__/admin").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problemAuthenticationEntryPoint)
                        .accessDeniedHandler(problemAccessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(problemAuthenticationEntryPoint)
                        .accessDeniedHandler(problemAccessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .build();
    }

    @RestController
    public static class ProbeController {
        @GetMapping("/__test__/analyst")
        ResponseEntity<Void> analyst() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/__test__/admin")
        ResponseEntity<Void> admin() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/__production-deny-probe")
        ResponseEntity<Void> productionDenyProbe() {
            return ResponseEntity.ok().build();
        }
    }
}
