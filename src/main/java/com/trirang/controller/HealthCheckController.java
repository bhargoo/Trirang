package com.trirang.controller;

import com.trirang.model.dto.HealthCheckResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
public class HealthCheckController {

    @GetMapping
    public ResponseEntity<HealthCheckResponse> getHealth() {
        HealthCheckResponse response = new HealthCheckResponse(
            "UP",
            "TriRang Monolith Service",
            Instant.now()
        );
        return ResponseEntity.ok(response);
    }
}
