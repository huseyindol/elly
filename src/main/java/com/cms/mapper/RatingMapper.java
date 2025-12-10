package com.cms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.cms.dto.DtoRating;
import com.cms.dto.DtoRatingIU;
import com.cms.entity.Rating;

@Mapper(componentModel = "spring")
public interface RatingMapper {

  DtoRating toDtoRating(Rating rating);

  @Mapping(target = "post", ignore = true)
  @Mapping(target = "userIdentifier", ignore = true)
  Rating toRating(DtoRatingIU dtoRatingIU);

  List<DtoRating> toDtoRatings(List<Rating> ratings);
}
