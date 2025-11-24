package com.cms.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cms.entity.Comment;

@Service
public interface ICommentService {
  Comment saveComment(Comment comment);

  Boolean deleteComment(Long id);

  Comment getCommentById(Long id);

  List<Comment> getCommentsByPostId(Long postId);

  // List<Comment> getCommentsByArticleId(Long articleId);

}
