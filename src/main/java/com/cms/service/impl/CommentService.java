package com.cms.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cms.entity.Comment;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.CommentRepository;
import com.cms.service.ICommentService;

@Service
public class CommentService implements ICommentService {

  @Autowired
  private CommentRepository commentRepository;

  @Override
  public Boolean deleteComment(Long id) {
    if (commentRepository.existsById(id)) {
      commentRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  public List<Comment> getCommentsByPostId(Long postId) {
    return commentRepository.findByPostId(postId);
  }

  @Override
  public List<Comment> getTopLevelCommentsByPostId(Long postId) {
    return commentRepository.findByPostIdAndParentCommentIsNull(postId);
  }

  @Override
  public Comment saveComment(Comment comment) {
    Comment savedComment = commentRepository.save(comment);
    return savedComment;
  }

  @Override
  public Comment getCommentById(Long id) {
    return commentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Comment", id));
  }

}
