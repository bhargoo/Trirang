package com.trirang.service;

import com.trirang.model.dto.AuthResponse;
import com.trirang.model.dto.LoginRequest;
import com.trirang.model.dto.RefreshTokenRequest;
import com.trirang.model.dto.RegisterRequest;
import com.trirang.model.entity.User;
import com.trirang.model.enums.shared.Role;
import com.trirang.model.enums.shared.VerificationBadge;
import com.trirang.repository.UserRepository;
import com.trirang.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            log.warn("Blocked public registration attempt for ADMIN role with email: {}", request.email());
            throw new IllegalArgumentException("Public registration for ADMIN role is strictly forbidden.");
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalStateException("Email address already registered.");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .phone(request.phone())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .address(request.address())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .trustScore(100)
                .isBanned(false)
                .verificationBadge(VerificationBadge.NONE)
                .build();

        userRepository.save(user);
        log.info("Registered user successfully with email: {}", request.email());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials.");
        }

        if (user.getIsBanned()) {
            throw new IllegalStateException("Your account is currently banned.");
        }

        UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);

        // Store refresh token in Redis with a 7-day TTL
        redisTemplate.opsForValue().set("rt:" + refreshToken, user.getEmail(), Duration.ofDays(7));
        log.info("Logged in user successfully and saved refresh token in Redis: {}", user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getTrustScore()
        );
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();

        // Retrieve user email associated with this refresh token from Redis
        String email = redisTemplate.opsForValue().get("rt:" + refreshToken);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or expired refresh token.");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        if (!jwtService.isTokenValid(refreshToken, userDetails)) {
            redisTemplate.delete("rt:" + refreshToken);
            throw new IllegalArgumentException("Invalid refresh token.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User associated with token no longer exists."));

        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        // Rotate refresh token: delete old, store new with 7-day TTL
        redisTemplate.delete("rt:" + refreshToken);
        redisTemplate.opsForValue().set("rt:" + newRefreshToken, email, Duration.ofDays(7));

        log.info("Rotated refresh token successfully for user: {}", email);

        return new AuthResponse(
                newAccessToken,
                newRefreshToken,
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getTrustScore()
        );
    }
}
