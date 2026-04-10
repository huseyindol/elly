package com.cms.controller.impl;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.prepost.PreAuthorize;

import com.cms.controller.IRatingController;
import com.cms.dto.DtoRating;
import com.cms.dto.DtoRatingIU;
import com.cms.dto.DtoRatingStats;
import com.cms.entity.Post;
import com.cms.entity.Rating;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.RatingMapper;
import com.cms.service.IPostService;
import com.cms.service.IRatingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController extends BaseController implements IRatingController {

  private final IRatingService ratingService;
  private final RatingMapper ratingMapper;
  private final IPostService postService;

  @Override
  @PostMapping
  @PreAuthorize("hasAuthority('ratings:create')")
  public RootEntityResponse<DtoRating> saveRating(
      @Valid @RequestBody DtoRatingIU dtoRatingIU,
      HttpServletRequest request) {

    Rating rating = ratingMapper.toRating(dtoRatingIU);

    if (dtoRatingIU.getPostId() != null) {
      Post post = postService.getPostById(dtoRatingIU.getPostId());
      rating.setPost(post);
    } else {
      return error("Post ID is required");
    }

    // User identifier (IP adresi)
    String userIp = request.getRemoteAddr();
    rating.setUserIdentifier(userIp);

    Rating savedRating = ratingService.saveRating(rating);
    DtoRating dtoRating = ratingMapper.toDtoRating(savedRating);
    return ok(dtoRating);
  }

  @Override
  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('ratings:read')")
  public RootEntityResponse<DtoRating> getRatingById(@PathVariable Long id) {
    Rating rating = ratingService.getRatingById(id);
    DtoRating dtoRating = ratingMapper.toDtoRating(rating);
    return ok(dtoRating);
  }

  @Override
  @GetMapping("/post/{postId}")
  @PreAuthorize("hasAuthority('ratings:read')")
  public RootEntityResponse<List<DtoRating>> getRatingsByPostId(@PathVariable Long postId) {
    List<Rating> ratings = ratingService.getRatingsByPostId(postId);
    List<DtoRating> dtoRatings = ratingMapper.toDtoRatings(ratings);
    return ok(dtoRatings);
  }

  @Override
  @GetMapping("/stats/{postId}")
  @PreAuthorize("hasAuthority('ratings:read')")
  public RootEntityResponse<DtoRatingStats> getRatingStats(@PathVariable Long postId) {
    DtoRatingStats stats = new DtoRatingStats();
    stats.setPostId(postId);
    stats.setAverageRating(ratingService.getAverageRating(postId));
    stats.setTotalRatings(ratingService.getRatingCount(postId));
    return ok(stats);
  }
}
