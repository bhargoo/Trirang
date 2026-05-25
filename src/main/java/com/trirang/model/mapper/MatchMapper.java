package com.trirang.model.mapper;

import com.trirang.model.dto.MatchResponse;
import com.trirang.model.entity.Match;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    @Mapping(target = "donationId", source = "donation.id")
    @Mapping(target = "donationTitle", source = "donation.title")
    @Mapping(target = "artisanRequirementId", source = "artisanRequirement.id")
    @Mapping(target = "artisanRequirementMaterial", source = "artisanRequirement.material")
    @Mapping(target = "donorId", source = "donor.id")
    @Mapping(target = "donorName", source = "donor.fullName")
    @Mapping(target = "artisanId", source = "artisan.id")
    @Mapping(target = "artisanName", source = "artisan.fullName")
    MatchResponse toResponse(Match match);
}
