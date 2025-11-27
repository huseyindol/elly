package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoComment;
import com.cms.dto.DtoCommentIU;
import com.cms.entity.RootEntityResponse;

public interface ICommentController {

  RootEntityResponse<DtoComment> saveComment(DtoCommentIU dtoCommentIU);

  RootEntityResponse<Boolean> deleteComment(Long id);

  RootEntityResponse<DtoComment> getCommentById(Long id);

  RootEntityResponse<List<DtoComment>> getCommentsByPostId(Long postId);

}
