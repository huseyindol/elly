package com.cms.service.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.entity.Rating;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.RatingRepository;
import com.cms.service.IRatingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RatingService implements IRatingService {

  private final RatingRepository ratingRepository;

  @Override
  @Transactional
  @CacheEvict(value = "ratings", allEntries = true)
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
  @Cacheable(value = "ratings", key = "#id")
  public Rating getRatingById(Long id) {
    return ratingRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Rating", id));
  }

  @Override
  @Cacheable(value = "ratings", key = "'byPost-' + #postId")
  public List<Rating> getRatingsByPostId(Long postId) {
    return ratingRepository.findByPostId(postId);
  }

  @Override
  @Cacheable(value = "ratings", key = "'avg-' + #postId")
  public Double getAverageRating(Long postId) {
    Double avg = ratingRepository.getAverageRating(postId);
    return avg != null ? avg : 0.0;
  }

  @Override
  @Cacheable(value = "ratings", key = "'count-' + #postId")
  public Long getRatingCount(Long postId) {
    return ratingRepository.getRatingCount(postId);
  }
}
