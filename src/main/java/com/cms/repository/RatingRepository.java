package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.cms.entity.Rating;

public interface RatingRepository extends JpaRepository<Rating, Long> {

  List<Rating> findByPostId(Long postId);

  Optional<Rating> findByPostIdAndUserIdentifier(Long postId, String userIdentifier);

  @Query("SELECT AVG(r.rating) FROM Rating r WHERE r.post.id = :postId")
  Double getAverageRating(@Param("postId") Long postId);

  @Query("SELECT COUNT(r) FROM Rating r WHERE r.post.id = :postId")
  Long getRatingCount(@Param("postId") Long postId);
}
