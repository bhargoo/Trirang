package com.trirang.model.mapper;

import com.trirang.model.dto.ReviewResponse;
import com.trirang.model.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    @Mapping(target = "reviewerId", source = "reviewer.id")
    @Mapping(target = "reviewerName", source = "reviewer.fullName")
    @Mapping(target = "reviewedUserId", source = "reviewedUser.id")
    @Mapping(target = "reviewedUserName", source = "reviewedUser.fullName")
    @Mapping(target = "relatedMatchId", source = "relatedMatch.id")
    ReviewResponse toResponse(Review review);
}
