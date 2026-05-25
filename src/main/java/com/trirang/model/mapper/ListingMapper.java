package com.trirang.model.mapper;

import com.trirang.model.dto.ListingResponse;
import com.trirang.model.dto.SellerPreviewDTO;
import com.trirang.model.entity.Listing;
import com.trirang.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ListingMapper {

    @Mapping(target = "sellerId", source = "seller.id")
    @Mapping(target = "seller", source = "seller")
    ListingResponse toResponse(Listing listing);

    default SellerPreviewDTO mapSeller(User seller) {
        if (seller == null) {
            return null;
        }
        return new SellerPreviewDTO(
                seller.getFullName(),
                seller.getTrustScore() != null ? seller.getTrustScore().doubleValue() : 0.0,
                seller.getVerificationBadge() != com.trirang.model.enums.VerificationBadge.NONE
        );
    }
}
