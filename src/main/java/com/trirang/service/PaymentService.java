package com.trirang.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
public class PaymentService {

    /**
     * Integrates with Razorpay SDK or API to create an order.
     * Stubs/mocks locally to ensure localhost-first operation before Phase 10 integration is fully setup.
     */
    public String createRazorpayOrder(BigDecimal amount) {
        log.info("Creating Razorpay order for amount: {}", amount);
        
        // In full integration this would instantiate the RazorpayClient:
        // RazorpayClient client = new RazorpayClient(keyId, keySecret);
        // Order order = client.orders.create(orderRequestJson);
        // return order.get("id");
        
        String mockRazorpayOrderId = "order_" + UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        log.info("Successfully created mock Razorpay order: {}", mockRazorpayOrderId);
        return mockRazorpayOrderId;
    }

    /**
     * Verifies the Razorpay payment signature.
     */
    public boolean verifyPaymentSignature(String paymentId, String orderId, String signature) {
        log.info("Verifying Razorpay payment signature for paymentId: {}, orderId: {}", paymentId, orderId);
        
        // In full integration this would verify signature using:
        // Utils.verifyPaymentSignature(payload, signature, apiSecret);
        
        if (signature == null || signature.isBlank()) {
            log.error("Payment signature verification failed: signature is empty");
            return false;
        }

        log.info("Razorpay payment signature verified successfully!");
        return true;
    }
}
