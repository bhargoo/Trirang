package com.trirang.model.mapper;

import com.trirang.model.dto.RecyclerClaimResponse;
import com.trirang.model.entity.RecyclerClaim;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RecyclerClaimMapper {

    @Mapping(target = "recyclerId", source = "recycler.id")
    @Mapping(target = "recyclerName", source = "recycler.fullName")
    @Mapping(target = "donationId", source = "donation.id")
    @Mapping(target = "donationTitle", source = "donation.title")
    RecyclerClaimResponse toResponse(RecyclerClaim claim);
}
