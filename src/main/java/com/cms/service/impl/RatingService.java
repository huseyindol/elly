package com.cms.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cms.entity.Rating;
import com.cms.repository.RatingRepository;
import com.cms.service.IRatingService;

@Service
public class RatingService implements IRatingService {

  @Autowired
  private RatingRepository ratingRepository;

  @Override
  public Rating saveRating(Rating rating) {
    // Aynı kullanıcı daha önce oy verdiyse güncelle
    Optional<Rating> existing = ratingRepository
        .findByPostIdAndUserIdentifier(
            rating.getPost().getId(),
            rating.getUserIdentifier());

    if (existing.isPresent()) {
      Rating existingRating = existing.get();
      existingRating.setRating(rating.getRating());
      existingRating.setComment(rating.getComment());
      return ratingRepository.save(existingRating);
    }

    return ratingRepository.save(rating);
  }

  @Override
  public Rating getRatingById(Long id) {
    return ratingRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Rating not found"));
  }

  @Override
  public List<Rating> getRatingsByPostId(Long postId) {
    return ratingRepository.findByPostId(postId);
  }

  @Override
  public Double getAverageRating(Long postId) {
    Double avg = ratingRepository.getAverageRating(postId);
    return avg != null ? avg : 0.0;
  }

  @Override
  public Long getRatingCount(Long postId) {
    return ratingRepository.getRatingCount(postId);
  }
}
