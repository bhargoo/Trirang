package com.trirang.repository;

import com.trirang.model.entity.MarketplaceOrder;
import com.trirang.model.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrder, UUID> {
    List<MarketplaceOrder> findByBuyerId(UUID buyerId);
    List<MarketplaceOrder> findByListingSellerId(UUID sellerId);
    List<MarketplaceOrder> findByStatusAndCreatedAtBefore(OrderStatus status, Instant expirationTime);
    boolean existsByBuyerIdAndListingIdAndStatusIn(UUID buyerId, UUID listingId, Collection<OrderStatus> statuses);
}
