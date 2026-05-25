package com.trirang.controller;

import com.trirang.model.dto.request.QrVerificationRequest;
import com.trirang.model.dto.response.QrGenerationResponse;
import com.trirang.model.dto.response.QrVerificationResponse;
import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import com.trirang.service.QrService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pickups")
@RequiredArgsConstructor
public class PickupController {

    private final QrService qrService;
    private final UserRepository userRepository;

    @PostMapping("/{matchId}/generate-qr")
    public ResponseEntity<QrGenerationResponse> generateQr(
            @PathVariable UUID matchId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        return ResponseEntity.ok(qrService.generateQrForMatch(matchId, user.getId()));
    }

    @PostMapping("/{matchId}/verify")
    public ResponseEntity<QrVerificationResponse> verifyQr(
            @PathVariable UUID matchId,
            @Valid @RequestBody QrVerificationRequest request) {
            
        return ResponseEntity.ok(qrService.verifyQrForMatch(request.token(), matchId));
    }
}
