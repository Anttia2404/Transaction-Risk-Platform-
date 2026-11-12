package com.votantai.transactionrisk.auth.api;

import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.votantai.transactionrisk.auth.application.AuthService;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.username().length() > 100 || request.password() == null
                || request.password().isBlank() || request.password().length() > 200
                || request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body(new ApiProblem(
                    "about:blank", "Bad Request", 400, "VALIDATION_ERROR", "Request validation failed",
                    "/api/v1/auth/login", null, java.util.List.of()));
        }
        var result = authService.login(request.username(), request.password());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(new LoginResponse(result.accessToken(), "Bearer", result.expiresIn(), result.user()));
    }

    @GetMapping("/me")
    public AuthService.UserSummary currentUser(JwtAuthenticationToken authentication) {
        UUID userId;
        try {
            userId = UUID.fromString(authentication.getToken().getSubject());
        } catch (IllegalArgumentException exception) {
            throw new com.votantai.transactionrisk.auth.application.CurrentUserUnavailableException();
        }
        return authService.currentUser(userId);
    }

    public record LoginResponse(String accessToken, String tokenType, long expiresIn, AuthService.UserSummary user) {
        public LoginResponse {
            if (expiresIn <= 0) {
                throw new IllegalArgumentException("JWT expiry must be positive");
            }
        }
    }
}
