package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoPost;
import com.cms.dto.DtoPostIU;
import com.cms.dto.DtoPostSummary;
import com.cms.dto.PagedResponse;
import com.cms.entity.RootEntityResponse;

public interface IPostController {
  RootEntityResponse<DtoPost> createPost(DtoPostIU dtoPostIU);

  RootEntityResponse<DtoPost> updatePost(Long id, DtoPostIU dtoPostIU);

  RootEntityResponse<Boolean> deletePost(Long id);

  RootEntityResponse<DtoPost> getPostById(Long id);

  RootEntityResponse<List<DtoPost>> getAllPosts();

  RootEntityResponse<List<DtoPostSummary>> getAllPostsSummary();

  RootEntityResponse<DtoPost> getPostBySlug(String slug);

  // Paginated endpoints
  RootEntityResponse<PagedResponse<DtoPost>> getAllPostsPaged(int page, int size, String sort);

  RootEntityResponse<PagedResponse<DtoPostSummary>> getAllPostsSummaryPaged(int page, int size, String sort);
}
