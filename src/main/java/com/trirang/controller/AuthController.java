package com.trirang.controller;

import com.trirang.model.dto.AuthResponse;
import com.trirang.model.dto.LoginRequest;
import com.trirang.model.dto.RefreshTokenRequest;
import com.trirang.model.dto.RegisterRequest;
import com.trirang.security.RateLimiterService;
import com.trirang.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiterService rateLimiterService;

    public AuthController(AuthService authService, RateLimiterService rateLimiterService) {
        this.authService = authService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpServletRequest) {

        String ip = httpServletRequest.getRemoteAddr();
        if (!rateLimiterService.tryConsume(ip)) {
            log.warn("Rate limit exceeded for IP: {} on registration", ip);
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many registration attempts. Please try again later."
            );
            problemDetail.setTitle("Too Many Requests");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problemDetail);
        }

        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest) {

        String ip = httpServletRequest.getRemoteAddr();
        if (!rateLimiterService.tryConsume(ip)) {
            log.warn("Rate limit exceeded for IP: {} on login", ip);
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts. Please try again later."
            );
            problemDetail.setTitle("Too Many Requests");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problemDetail);
        }

        AuthResponse authResponse = authService.login(request);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpServletRequest) {

        String ip = httpServletRequest.getRemoteAddr();
        if (!rateLimiterService.tryConsume(ip)) {
            log.warn("Rate limit exceeded for IP: {} on token refresh", ip);
            ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many refresh requests. Please try again later."
            );
            problemDetail.setTitle("Too Many Requests");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(problemDetail);
        }

        AuthResponse authResponse = authService.refresh(request);
        return ResponseEntity.ok(authResponse);
    }
}
