package com.trirang.model.mapper;

import com.trirang.model.dto.MatchResponse;
import com.trirang.model.entity.Match;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MatchMapper {

    @Mapping(target = "donationId", source = "donation.id")
    @Mapping(target = "donationTitle", source = "donation.title")
    @Mapping(target = "donorName", source = "donation.donor.fullName")
    @Mapping(target = "requirementId", source = "requirement.id")
    @Mapping(target = "requirementMaterial", source = "requirement.material")
    @Mapping(target = "artisanName", source = "requirement.artisan.fullName")
    MatchResponse toResponse(Match match);
}
