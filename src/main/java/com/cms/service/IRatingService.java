package com.cms.service;

import java.util.List;

import com.cms.entity.Rating;

public interface IRatingService {
  Rating saveRating(Rating rating);

  Rating getRatingById(Long id);

  List<Rating> getRatingsByPostId(Long postId);

  Double getAverageRating(Long postId);

  Long getRatingCount(Long postId);
}
