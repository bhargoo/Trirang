package com.trirang.config;

import com.trirang.model.entity.User;
import com.trirang.model.enums.shared.Role;
import com.trirang.model.enums.shared.VerificationBadge;
import com.trirang.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        long adminCount = userRepository.findAll().stream()
                .filter(user -> user.getRole() == Role.ADMIN)
                .count();

        if (adminCount == 0) {
            User admin = User.builder()
                    .name("TriRang System Admin")
                    .email("admin@trirang.com")
                    .phone("+919999999999")
                    .password(passwordEncoder.encode("Admin@Trirang123"))
                    .role(Role.ADMIN)
                    .trustScore(100)
                    .isBanned(false)
                    .verificationBadge(VerificationBadge.PLATINUM)
                    .address("TriRang Head Office, Mumbai, India")
                    .latitude(new BigDecimal("19.0760"))
                    .longitude(new BigDecimal("72.8777"))
                    .build();

            userRepository.save(admin);
            log.info("Successfully bootstrapped default Administrator account: admin@trirang.com");
        } else {
            log.info("Administrator accounts already exist, bypassing seed initialization.");
        }
    }
}
