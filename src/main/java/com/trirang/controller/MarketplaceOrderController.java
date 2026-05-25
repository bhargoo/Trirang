package com.trirang.controller;

import com.trirang.model.dto.*;
import com.trirang.model.entity.User;
import com.trirang.repository.UserRepository;
import com.trirang.service.MarketplaceOrderService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/marketplace/orders")
public class MarketplaceOrderController {

    private final MarketplaceOrderService orderService;
    private final UserRepository userRepository;

    public MarketplaceOrderController(MarketplaceOrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("Received request to place marketplace order on listing ID: {}", request.listingId());
        User currentUser = getCurrentUser();
        OrderResponse response = orderService.createOrder(currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<OrderResponse>> getMyOrders() {
        log.info("Received request to fetch current user's purchased orders");
        User currentUser = getCurrentUser();
        List<OrderResponse> responses = orderService.getMyOrders(currentUser);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/sales")
    public ResponseEntity<List<OrderResponse>> getSalesOrders() {
        log.info("Received request to fetch current user's listing sales orders");
        User currentUser = getCurrentUser();
        List<OrderResponse> responses = orderService.getSalesOrders(currentUser);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<OrderResponse> verifyPayment(
            @PathVariable("id") UUID id,
            @RequestParam(value = "version", required = false) Long version,
            @Valid @RequestBody VerifyPaymentRequest request) {

        log.info("Received request to verify payment for order ID: {} with version: {}", id, version);
        User currentUser = getCurrentUser();
        OrderResponse response = orderService.verifyPayment(id, currentUser, request, version);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/confirm-delivery")
    public ResponseEntity<OrderResponse> confirmDelivery(@PathVariable("id") UUID id) {
        log.info("Received request to confirm delivery for order ID: {}", id);
        User currentUser = getCurrentUser();
        OrderResponse response = orderService.confirmDelivery(id, currentUser);
        return ResponseEntity.ok(response);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("User is not authenticated");
        }
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found in database"));
    }
}
