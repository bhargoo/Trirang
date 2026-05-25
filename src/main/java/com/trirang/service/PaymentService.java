package com.trirang.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.trirang.model.dto.request.OrderRequest;
import com.trirang.model.dto.request.PaymentVerificationRequest;
import com.trirang.model.dto.response.OrderResponse;
import com.trirang.model.entity.PaymentTransaction;
import com.trirang.model.entity.User;
import com.trirang.model.entity.WebhookEvent;
import com.trirang.model.enums.shared.PaymentStatus;
import com.trirang.repository.PaymentTransactionRepository;
import com.trirang.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final WebhookEventRepository webhookEventRepository;

    @Value("${razorpay.key.secret}")
    private String razorpaySecret;

    @Value("${razorpay.webhook.secret}")
    private String webhookSecret;

    @Transactional
    public OrderResponse createOrder(User user, OrderRequest request) {
        try {
            JSONObject orderRequest = new JSONObject();
            // Razorpay expects amount in paise (multiply by 100)
            int amountInPaise = request.amount().multiply(new java.math.BigDecimal("100")).intValue();
            
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", request.currency());
            orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            PaymentTransaction transaction = PaymentTransaction.builder()
                    .user(user)
                    .amount(request.amount())
                    .currency(request.currency())
                    .razorpayOrderId(orderId)
                    .status(PaymentStatus.CREATED)
                    .build();

            paymentTransactionRepository.save(transaction);

            return OrderResponse.builder()
                    .razorpayOrderId(orderId)
                    .amount(request.amount())
                    .currency(request.currency())
                    .build();

        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order", e);
            throw new RuntimeException("Failed to create payment order");
        }
    }

    @Transactional
    public void verifyPayment(PaymentVerificationRequest request) {
        try {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.razorpayOrderId());
            options.put("razorpay_payment_id", request.razorpayPaymentId());
            options.put("razorpay_signature", request.razorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(options, razorpaySecret);

            if (isValid) {
                PaymentTransaction transaction = paymentTransactionRepository
                        .findByRazorpayOrderId(request.razorpayOrderId())
                        .orElseThrow(() -> new RuntimeException("Transaction not found"));

                transaction.setRazorpayPaymentId(request.razorpayPaymentId());
                transaction.setStatus(PaymentStatus.SUCCESS);
                paymentTransactionRepository.save(transaction);
                log.info("Payment verified successfully for order: {}", request.razorpayOrderId());
            } else {
                throw new RuntimeException("Payment signature verification failed");
            }
        } catch (RazorpayException e) {
            log.error("Razorpay verification failed", e);
            throw new RuntimeException("Payment verification failed", e);
        }
    }

    @Async
    @Transactional
    public void processWebhook(String rawBody, String signature) {
        try {
            // Verify webhook authenticity
            boolean isValid = Utils.verifyWebhookSignature(rawBody, signature, webhookSecret);
            if (!isValid) {
                log.warn("Invalid webhook signature received");
                return;
            }

            JSONObject payload = new JSONObject(rawBody);
            String event = payload.getString("event");
            
            // Check for idempotency using Razorpay's unique header (usually sent via headers, but let's parse from payload if needed)
            // Razorpay often sends x-razorpay-event-id header, but here we can generate one from payload ID if missing.
            // Wait, Razorpay webhooks have 'account_id' and 'event'. The header contains 'x-razorpay-event-id'.
            // Let's assume the controller passed the header or we can just parse the payment ID.
            // Actually, for simplicity if we don't have eventId, we can use the payment ID + event type as idempotency key.
            // We'll update the controller to pass eventId if possible, but let's just parse the payload for now.

            JSONObject payloadContent = payload.getJSONObject("payload");
            
            if ("payment.captured".equals(event)) {
                JSONObject paymentEntity = payloadContent.getJSONObject("payment").getJSONObject("entity");
                String orderId = paymentEntity.getString("order_id");
                String paymentId = paymentEntity.getString("id");

                // Idempotency check: if we already processed this paymentId for captured event, skip.
                String idempotencyKey = "captured_" + paymentId;
                if (webhookEventRepository.existsById(idempotencyKey)) {
                    log.info("Webhook event already processed: {}", idempotencyKey);
                    return;
                }

                // Save event to prevent duplicate processing
                webhookEventRepository.save(new WebhookEvent(idempotencyKey, Instant.now()));

                paymentTransactionRepository.findByRazorpayOrderId(orderId).ifPresent(transaction -> {
                    if (transaction.getStatus() != PaymentStatus.SUCCESS) {
                        transaction.setStatus(PaymentStatus.SUCCESS);
                        transaction.setRazorpayPaymentId(paymentId);
                        paymentTransactionRepository.save(transaction);
                        log.info("Payment captured via webhook for order: {}", orderId);
                    }
                });
            } else if ("payment.failed".equals(event)) {
                JSONObject paymentEntity = payloadContent.getJSONObject("payment").getJSONObject("entity");
                String orderId = paymentEntity.getString("order_id");
                String paymentId = paymentEntity.getString("id");

                String idempotencyKey = "failed_" + paymentId;
                if (webhookEventRepository.existsById(idempotencyKey)) {
                    return;
                }
                webhookEventRepository.save(new WebhookEvent(idempotencyKey, Instant.now()));

                paymentTransactionRepository.findByRazorpayOrderId(orderId).ifPresent(transaction -> {
                    transaction.setStatus(PaymentStatus.FAILED);
                    transaction.setRazorpayPaymentId(paymentId);
                    paymentTransactionRepository.save(transaction);
                    log.info("Payment failed via webhook for order: {}", orderId);
                });
            }
            
            // Other events can be handled here

        } catch (Exception e) {
            log.error("Failed to process webhook", e);
        }
    }
}
