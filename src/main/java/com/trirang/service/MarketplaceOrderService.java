package com.trirang.service;

import com.trirang.model.dto.*;
import com.trirang.model.entity.Listing;
import com.trirang.model.entity.MarketplaceOrder;
import com.trirang.model.entity.User;
import com.trirang.model.enums.ListingStatus;
import com.trirang.model.enums.OrderStatus;
import com.trirang.model.mapper.MarketplaceOrderMapper;
import com.trirang.repository.ListingRepository;
import com.trirang.repository.MarketplaceOrderRepository;
import com.trirang.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class MarketplaceOrderService {

    private final MarketplaceOrderRepository orderRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final PaymentService paymentService;
    private final MarketplaceOrderMapper orderMapper;

    public MarketplaceOrderService(
            MarketplaceOrderRepository orderRepository,
            ListingRepository listingRepository,
            UserRepository userRepository,
            PaymentService paymentService,
            MarketplaceOrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.paymentService = paymentService;
        this.orderMapper = orderMapper;
    }

    public OrderResponse createOrder(User buyer, CreateOrderRequest request) {
        log.info("Creating order for buyer: {} on listing: {}", buyer.getId(), request.listingId());

        Listing listing = listingRepository.findById(request.listingId())
                .orElseThrow(() -> new IllegalArgumentException("Listing not found with ID: " + request.listingId()));

        // Buyer cannot purchase own listing
        if (listing.getSeller().getId().equals(buyer.getId())) {
            throw new IllegalArgumentException("You cannot purchase your own listing");
        }

        // Listing must be ACTIVE
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            throw new IllegalStateException("This listing is no longer available for purchase");
        }

        // Prevent duplicate purchases
        boolean activeOrderExists = orderRepository.existsByBuyerIdAndListingIdAndStatusIn(
                buyer.getId(),
                listing.getId(),
                List.of(OrderStatus.PENDING, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED)
        );

        if (activeOrderExists) {
            throw new IllegalStateException("You already have an active order or purchase for this listing");
        }

        // Lock listing as RESERVED
        listing.setStatus(ListingStatus.RESERVED);
        listingRepository.save(listing);

        // Create Razorpay order
        String razorpayOrderId = paymentService.createRazorpayOrder(listing.getPrice());

        MarketplaceOrder order = MarketplaceOrder.builder()
                .buyer(buyer)
                .listing(listing)
                .amount(listing.getPrice())
                .status(OrderStatus.PENDING)
                .razorpayOrderId(razorpayOrderId)
                .deliveryAddress(request.deliveryAddress())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        MarketplaceOrder saved = orderRepository.save(order);
        log.info("Successfully created marketplace order: {} locked in PENDING state", saved.getId());

        return orderMapper.toResponse(saved);
    }

    public OrderResponse verifyPayment(UUID orderId, User buyer, VerifyPaymentRequest request, Long expectedVersion) {
        log.info("Verifying payment for order ID: {}", orderId);

        MarketplaceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        // Enforce ownership
        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new IllegalStateException("You are not authorized to verify payment for this order");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can have their payments verified");
        }

        // Optimistic locking verification
        if (expectedVersion != null && !expectedVersion.equals(order.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(MarketplaceOrder.class, orderId);
        }

        // Verify Razorpay signature
        boolean isValid = paymentService.verifyPaymentSignature(
                request.razorpayPaymentId(),
                request.razorpayOrderId(),
                request.razorpaySignature()
        );

        if (!isValid) {
            throw new IllegalArgumentException("Invalid payment signature or credentials");
        }

        // Transition order status to PAID
        order.setStatus(OrderStatus.PAID);
        order.setUpdatedAt(Instant.now());
        MarketplaceOrder saved = orderRepository.save(order);

        // Transition listing status to SOLD
        Listing listing = order.getListing();
        listing.setStatus(ListingStatus.SOLD);
        listingRepository.save(listing);

        // Async/Background notification logic simulation
        log.info("[NOTIFICATION] Notification sent asynchronously to seller {} for successful sale of listing '{}' with order ID: {}",
                listing.getSeller().getFullName(), listing.getTitle(), order.getId());

        return orderMapper.toResponse(saved);
    }

    public OrderResponse confirmDelivery(UUID orderId, User buyer) {
        log.info("Confirming delivery for order ID: {} by buyer: {}", orderId, buyer.getId());

        MarketplaceOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with ID: " + orderId));

        if (!order.getBuyer().getId().equals(buyer.getId())) {
            throw new IllegalStateException("Only the buyer can confirm delivery of this order");
        }

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Delivery can only be confirmed for PAID or SHIPPED orders");
        }

        // Transition order status to DELIVERED
        order.setStatus(OrderStatus.DELIVERED);
        order.setUpdatedAt(Instant.now());
        MarketplaceOrder saved = orderRepository.save(order);

        // Update seller trust score (+5 up to 100)
        User seller = order.getListing().getSeller();
        if (seller.getTrustScore() != null) {
            seller.setTrustScore(Math.min(100, seller.getTrustScore() + 5));
            userRepository.save(seller);
            log.info("Updated trust score for seller {}: new trust score = {}", seller.getFullName(), seller.getTrustScore());
        }

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(User buyer) {
        log.info("Fetching marketplace orders for buyer: {}", buyer.getId());
        return orderRepository.findByBuyerId(buyer.getId()).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getSalesOrders(User seller) {
        log.info("Fetching sales orders for seller: {}", seller.getId());
        return orderRepository.findByListingSellerId(seller.getId()).stream()
                .map(orderMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Reverts RESERVED -> ACTIVE if payment fails or order isn't completed after 15 minutes.
     * Generates a scheduled cleanup task running every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void cleanupExpiredReservations() {
        log.info("Running scheduled cleanup task for expired marketplace reservations...");

        Instant expirationTime = Instant.now().minus(Duration.ofMinutes(15));
        List<MarketplaceOrder> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.PENDING, expirationTime);

        for (MarketplaceOrder order : expiredOrders) {
            log.info("Order ID: {} has expired PENDING state. Reverting reservation.", order.getId());

            // Cancel the order
            order.setStatus(OrderStatus.CANCELLED);
            order.setUpdatedAt(Instant.now());
            orderRepository.save(order);

            // Revert listing to ACTIVE
            Listing listing = order.getListing();
            if (listing.getStatus() == ListingStatus.RESERVED) {
                listing.setStatus(ListingStatus.ACTIVE);
                listingRepository.save(listing);
                log.info("Reverted listing ID: {} back to ACTIVE", listing.getId());
            }
        }
    }
}
