package com.cms.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.entity.Comment;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.CommentRepository;
import com.cms.service.ICommentService;

@Service
public class CommentService implements ICommentService {

  @Autowired
  private CommentRepository commentRepository;

  @Override
  @Transactional
  @CacheEvict(value = "comments", allEntries = true)
  public Boolean deleteComment(Long id) {
    if (commentRepository.existsById(id)) {
      commentRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  @Cacheable(value = "comments", key = "'byPost-' + #postId")
  public List<Comment> getCommentsByPostId(Long postId) {
    return commentRepository.findByPostId(postId);
  }

  @Override
  @Cacheable(value = "comments", key = "'topByPost-' + #postId")
  public List<Comment> getTopLevelCommentsByPostId(Long postId) {
    return commentRepository.findByPostIdAndParentCommentIsNull(postId);
  }

  @Override
  @Transactional
  @CacheEvict(value = "comments", allEntries = true)
  public Comment saveComment(Comment comment) {
    Comment savedComment = commentRepository.save(comment);
    return savedComment;
  }

  @Override
  @Cacheable(value = "comments", key = "#id")
  public Comment getCommentById(Long id) {
    return commentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Comment", id));
  }

}
