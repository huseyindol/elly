package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.cms.dto.DtoPostSummary;
import com.cms.entity.Post;

public interface IPostService {
  Post savePost(Post post);

  Post getPostById(Long id);

  Boolean deletePost(Long id);

  List<Post> getAllPosts();

  List<DtoPostSummary> getAllPostsSummary();

  Post getPostBySlug(String slug);

  // Paginated methods
  Page<Post> getAllPostsPaged(Pageable pageable);

  Page<DtoPostSummary> getAllPostsSummaryPaged(Pageable pageable);
}
