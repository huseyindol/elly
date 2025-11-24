package com.cms.controller;

import com.cms.dto.DtoPost;
import com.cms.dto.DtoPostIU;
import com.cms.entity.RootEntityResponse;

public interface IPostController {
  RootEntityResponse<DtoPost> createPost(DtoPostIU dtoPostIU);

  RootEntityResponse<DtoPost> updatePost(Long id, DtoPostIU dtoPostIU);

  RootEntityResponse<Boolean> deletePost(Long id);

  RootEntityResponse<DtoPost> getPostById(Long id);

}
