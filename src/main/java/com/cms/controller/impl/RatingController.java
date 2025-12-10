package com.cms.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

@RestController
@RequestMapping("/api/v1/ratings")
public class RatingController extends BaseController implements IRatingController {

  @Autowired
  private IRatingService ratingService;

  @Autowired
  private RatingMapper ratingMapper;

  @Autowired
  private IPostService postService;

  @Override
  @PostMapping
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
  public RootEntityResponse<DtoRating> getRatingById(@PathVariable Long id) {
    Rating rating = ratingService.getRatingById(id);
    DtoRating dtoRating = ratingMapper.toDtoRating(rating);
    return ok(dtoRating);
  }

  @Override
  @GetMapping("/post/{postId}")
  public RootEntityResponse<List<DtoRating>> getRatingsByPostId(@PathVariable Long postId) {
    List<Rating> ratings = ratingService.getRatingsByPostId(postId);
    List<DtoRating> dtoRatings = ratingMapper.toDtoRatings(ratings);
    return ok(dtoRatings);
  }

  @Override
  @GetMapping("/stats/{postId}")
  public RootEntityResponse<DtoRatingStats> getRatingStats(@PathVariable Long postId) {
    DtoRatingStats stats = new DtoRatingStats();
    stats.setPostId(postId);
    stats.setAverageRating(ratingService.getAverageRating(postId));
    stats.setTotalRatings(ratingService.getRatingCount(postId));
    return ok(stats);
  }
}
