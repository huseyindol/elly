package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoRating;
import com.cms.dto.DtoRatingIU;
import com.cms.dto.DtoRatingStats;
import com.cms.entity.RootEntityResponse;

import jakarta.servlet.http.HttpServletRequest;

public interface IRatingController {
  RootEntityResponse<DtoRating> saveRating(DtoRatingIU dtoRatingIU, HttpServletRequest request);

  RootEntityResponse<DtoRating> getRatingById(Long id);

  RootEntityResponse<List<DtoRating>> getRatingsByPostId(Long postId);

  RootEntityResponse<DtoRatingStats> getRatingStats(Long postId);
}
