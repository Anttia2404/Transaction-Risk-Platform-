package com.votantai.transactionrisk;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import com.votantai.transactionrisk.auth.domain.Role;
import com.votantai.transactionrisk.auth.infrastructure.UserRepository;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Import(SecurityProbeConfiguration.class)
class FoundationIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");
    private static final String JWT_SECRET = createSecret();

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.auth.jwt.secret", () -> JWT_SECRET);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private Flyway flyway;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Test
    void startsWithPostgresAndAppliesFoundationMigration() throws Exception {
        var appliedMigration = Arrays.stream(flyway.info().applied())
                .filter(migration -> "1".equals(migration.getVersion().getVersion()))
                .findFirst();

        assertThat(appliedMigration).isPresent();
        assertThat(appliedMigration.orElseThrow().getState().isApplied()).isTrue();

        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health"))
                .GET()
                .build();
        var response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
        assertThat(flyway.info().applied()).extracting(migration -> migration.getVersion().getVersion())
                .contains("1", "2");
    }

    @Test
    void loginNormalizesUsernameAndReturnsJwtWithoutLeakingCredentials() throws Exception {
        var user = createUser("analyst", "ANALYST", true, "correct-horse");

        var login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"  ANALYST \",\"password\":\"correct-horse\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.user.id").value(user))
                .andExpect(jsonPath("$.user.username").value("analyst"))
                .andExpect(jsonPath("$.user.roles[0]").value("ANALYST"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();
        String token = com.jayway.jsonpath.JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user))
                .andExpect(jsonPath("$.username").value("analyst"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        assertThat(jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class,
                java.util.UUID.fromString(user))).startsWith("$2a$").doesNotContain("correct-horse");

        jdbcTemplate.update("UPDATE users SET enabled = FALSE WHERE id = ?", java.util.UUID.fromString(user));
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void badUnknownAndDisabledCredentialsHaveGenericUnauthorizedResponse() throws Exception {
        createUser("disabled", "ANALYST", false, "known-password");
        var wrong = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"username\":\"missing\",\"password\":\"guess\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        var disabled = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"username\":\"disabled\",\"password\":\"known-password\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        var badPassword = mockMvc.perform(post("/api/v1/auth/login").contentType("application/json")
                        .content("{\"username\":\"disabled\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        assertThat(wrong).contains("AUTHENTICATION_FAILED", "Invalid username or password");
        assertThat(disabled).contains("AUTHENTICATION_FAILED");
        assertThat(badPassword).contains("AUTHENTICATION_FAILED");
        assertThat(wrong).doesNotContain("guess", "known-password", "wrong-password");
    }

    @Test
    void localSeedInsertIsIdempotentAndDoesNotReplaceExistingCredentials() {
        String originalHash = passwordEncoder.encode("first-local-password");
        userRepository.insertIfAbsent(java.util.UUID.randomUUID(), "seed-user", originalHash, Role.ADMIN);
        userRepository.insertIfAbsent(java.util.UUID.randomUUID(), "  SEED-USER ",
                passwordEncoder.encode("replacement-password"), Role.ANALYST);

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE username = 'seed-user'", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT password_hash FROM users WHERE username = 'seed-user'", String.class))
                .isEqualTo(originalHash);
        assertThat(jdbcTemplate.queryForObject("SELECT role FROM users WHERE username = 'seed-user'", String.class))
                .isEqualTo("ADMIN");
    }

    @Test
    void bearerAuthenticationValidatesSignatureIssuerExpiryAndRoles() throws Exception {
        String analystToken = signedToken(JWT_SECRET, "ANALYST", "transaction-risk-platform", 900);
        mockMvc.perform(get("/__test__/analyst").header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/__test__/admin").header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isForbidden());

        String adminToken = signedToken(JWT_SECRET, "ADMIN", "transaction-risk-platform", 900);
        mockMvc.perform(get("/__test__/analyst").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/__production-deny-probe").header("Authorization", "Bearer " + analystToken))
                .andExpect(status().isForbidden());

        String wrongSignature = signedToken(createSecret(), "ANALYST", "transaction-risk-platform", 900);
        String wrongIssuer = signedToken(JWT_SECRET, "ANALYST", "other-issuer", 900);
        String expired = signedToken(JWT_SECRET, "ANALYST", "transaction-risk-platform", -30);
        String invalidRole = signedToken(JWT_SECRET, "SUPERUSER", "transaction-risk-platform", 900);
        for (String token : Arrays.asList(wrongSignature, wrongIssuer, expired, invalidRole)) {
            mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer malformed"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void healthIsPublicAndRequestsRemainStateless() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        var withoutBearer = mockMvc.perform(get("/api/v1/auth/me").header("Cookie", "JSESSIONID=anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();
        assertThat(withoutBearer.getRequest().getSession(false)).isNull();

        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "session-user", "", List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        var result = mockMvc.perform(get("/api/v1/auth/me").session(session).header("Cookie", "JSESSIONID=anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();
        assertThat(result.getRequest().getSession(false)).isSameAs(session);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("anything");
    }

    private String createUser(String username, String role, boolean enabled, String rawPassword) {
        java.util.UUID id = java.util.UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (id, username, password_hash, role, enabled) VALUES (?, ?, ?, ?, ?)",
                id, username, passwordEncoder.encode(rawPassword), role, enabled);
        return id.toString();
    }

    private static String createSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static String signedToken(String secret, String role, String issuer, long lifetimeSeconds) throws Exception {
        byte[] key = Base64.getDecoder().decode(secret);
        java.time.Instant now = java.time.Instant.now();
        var claims = new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(java.util.UUID.randomUUID().toString())
                .issueTime(java.util.Date.from(now))
                .notBeforeTime(java.util.Date.from(now))
                .expirationTime(java.util.Date.from(now.plusSeconds(lifetimeSeconds)))
                .claim("roles", List.of(role))
                .build();
        var jwt = new com.nimbusds.jwt.SignedJWT(
                new com.nimbusds.jose.JWSHeader(com.nimbusds.jose.JWSAlgorithm.HS256), claims);
        jwt.sign(new com.nimbusds.jose.crypto.MACSigner(key));
        return jwt.serialize();
    }
}
