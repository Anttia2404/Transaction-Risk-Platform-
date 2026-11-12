package com.votantai.transactionrisk.auth.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.votantai.transactionrisk.auth.domain.UserAccount;
import com.votantai.transactionrisk.auth.infrastructure.UserRepository;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final String issuer;
    private final long expirySeconds;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${app.auth.jwt.issuer}") String issuer,
            @Value("${app.auth.jwt.expiry-seconds:900}") long expirySeconds) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.issuer = issuer;
        this.expirySeconds = expirySeconds;
        if (expirySeconds <= 0) {
            throw new IllegalStateException("JWT expiry must be positive");
        }
    }

    public LoginResult login(String username, String password) {
        String normalizedUsername = UsernameNormalizer.normalize(username);
        if (normalizedUsername == null || password == null) {
            throw new AuthenticationFailedException();
        }
        try {
            authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(normalizedUsername, password));
        } catch (org.springframework.security.core.AuthenticationException exception) {
            throw new AuthenticationFailedException();
        }

        UserAccount user = userRepository.findByUsername(normalizedUsername)
                .filter(UserAccount::enabled)
                .orElseThrow(AuthenticationFailedException::new);
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(expirySeconds);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.id().toString())
                .issuedAt(now)
                .notBefore(now)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("roles", List.of(user.role().name()))
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResult(accessToken, expirySeconds, UserSummary.from(user));
    }

    public UserSummary currentUser(UUID userId) {
        return userRepository.findById(userId)
                .filter(UserAccount::enabled)
                .map(UserSummary::from)
                .orElseThrow(CurrentUserUnavailableException::new);
    }

    public record LoginResult(String accessToken, long expiresIn, UserSummary user) {
    }

    public record UserSummary(UUID id, String username, List<String> roles) {
        public static UserSummary from(UserAccount user) {
            return new UserSummary(user.id(), user.username(), List.of(user.role().name()));
        }
    }
}
