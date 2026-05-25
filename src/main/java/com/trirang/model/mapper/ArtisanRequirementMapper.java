package com.trirang.model.mapper;

import com.trirang.model.dto.ArtisanRequirementResponse;
import com.trirang.model.entity.ArtisanRequirement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArtisanRequirementMapper {

    @Mapping(target = "artisanId", source = "artisan.id")
    @Mapping(target = "artisanName", source = "artisan.fullName")
    ArtisanRequirementResponse toResponse(ArtisanRequirement requirement);
}
