package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.entity.Comment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findByPostId(Long postId);

  List<Comment> findByPostIdAndParentCommentIsNull(Long postId);

  Optional<Comment> findByEmail(String email);

}
