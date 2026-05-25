package com.trirang.model.mapper;

import com.trirang.model.dto.DonationResponse;
import com.trirang.model.entity.Donation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DonationMapper {

    @Mapping(target = "donorId", source = "donor.id")
    @Mapping(target = "donorName", source = "donor.fullName")
    @Mapping(target = "imageUrl", source = "imagePath")
    DonationResponse toResponse(Donation donation);
}
