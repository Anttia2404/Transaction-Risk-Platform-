package com.votantai.transactionrisk.auth.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.votantai.transactionrisk.auth.application.AuthenticationFailedException;
import com.votantai.transactionrisk.auth.application.CurrentUserUnavailableException;
import com.votantai.transactionrisk.auth.api.ApiProblem;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class AuthExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    ResponseEntity<ApiProblem> authenticationFailed(HttpServletRequest request) {
        return problem(request, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", "Invalid username or password");
    }

    @ExceptionHandler(CurrentUserUnavailableException.class)
    ResponseEntity<ApiProblem> currentUserUnavailable(HttpServletRequest request) {
        return problem(request, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "The authenticated user is unavailable");
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    ResponseEntity<ApiProblem> invalidRequest(HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed");
    }

    private ResponseEntity<ApiProblem> problem(HttpServletRequest request, HttpStatus status, String code, String detail) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(new ApiProblem("about:blank", status.getReasonPhrase(), status.value(), code,
                        detail, request.getRequestURI(), null, List.of()));
    }
}
