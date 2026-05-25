package com.trirang.controller;

import com.trirang.model.dto.request.OrderRequest;
import com.trirang.model.dto.request.PaymentVerificationRequest;
import com.trirang.model.dto.response.OrderResponse;
import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import com.trirang.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    @PostMapping("/create-order")
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
            
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        return ResponseEntity.ok(paymentService.createOrder(user, request));
    }

    @PostMapping("/verify")
    public ResponseEntity<Void> verifyPayment(
            @Valid @RequestBody PaymentVerificationRequest request) {
            
        paymentService.verifyPayment(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader("x-razorpay-signature") String signature) {
            
        paymentService.processWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
