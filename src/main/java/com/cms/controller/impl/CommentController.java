package com.cms.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.ICommentController;
import com.cms.dto.DtoComment;
import com.cms.dto.DtoCommentIU;
import com.cms.entity.Comment;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.CommentMapper;
import com.cms.service.ICommentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController extends BaseController implements ICommentController {

  @Autowired
  private ICommentService commentService;

  @Autowired
  private CommentMapper commentMapper;

  @Override
  @PostMapping
  public RootEntityResponse<DtoComment> saveComment(@Valid @RequestBody DtoCommentIU dtoCommentIU) {
    Comment comment = commentMapper.toComment(dtoCommentIU);
    Comment savedComment = commentService.saveComment(comment);
    DtoComment dtoComment = commentMapper.toDtoComment(savedComment);
    return ok(dtoComment);
  }

  @Override
  @DeleteMapping("/{id}")
  public RootEntityResponse<Boolean> deleteComment(@PathVariable Long id) {
    Boolean deleted = commentService.deleteComment(id);
    if (deleted) {
      return ok(deleted);
    }
    return error("Comment not deleted");
  }

  @Override
  @GetMapping("/post/{postId}")
  public RootEntityResponse<List<DtoComment>> getCommentsByPostId(@PathVariable Long postId) {
    List<Comment> comments = commentService.getCommentsByPostId(postId);
    List<DtoComment> dtoComments = commentMapper.toDtoComments(comments);
    return ok(dtoComments);
  }

  @Override
  @GetMapping("/{id}")
  public RootEntityResponse<DtoComment> getCommentById(@PathVariable Long id) {
    Comment comment = commentService.getCommentById(id);
    DtoComment dtoComment = commentMapper.toDtoComment(comment);
    return ok(dtoComment);
  }

}
