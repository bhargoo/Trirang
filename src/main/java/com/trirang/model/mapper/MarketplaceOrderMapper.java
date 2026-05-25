package com.trirang.model.mapper;

import com.trirang.model.dto.OrderResponse;
import com.trirang.model.entity.MarketplaceOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MarketplaceOrderMapper {

    @Mapping(target = "buyerId", source = "buyer.id")
    @Mapping(target = "buyerName", source = "buyer.fullName")
    @Mapping(target = "listingId", source = "listing.id")
    @Mapping(target = "listingTitle", source = "listing.title")
    OrderResponse toResponse(MarketplaceOrder order);
}
