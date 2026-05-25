package com.trirang.model.mapper;

import com.trirang.model.dto.TransformationResponse;
import com.trirang.model.entity.TransformationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransformationRequestMapper {

    @Mapping(target = "donorId", source = "donor.id")
    @Mapping(target = "donorName", source = "donor.fullName")
    @Mapping(target = "artisanId", source = "artisan.id")
    @Mapping(target = "artisanName", source = "artisan.fullName")
    @Mapping(target = "donationId", source = "donation.id")
    @Mapping(target = "donationTitle", source = "donation.title")
    TransformationResponse toResponse(TransformationRequest request);
}
