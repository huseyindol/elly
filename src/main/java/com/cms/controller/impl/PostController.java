package com.cms.controller.impl;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.IPostController;
import com.cms.dto.DtoPost;
import com.cms.dto.DtoPostIU;
import com.cms.dto.DtoPostSummary;
import com.cms.entity.Post;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.PostMapper;
import com.cms.service.IPostService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController extends BaseController implements IPostController {

  private final IPostService postService;
  private final PostMapper postMapper;

  @Override
  @PostMapping
  public RootEntityResponse<DtoPost> createPost(@RequestBody DtoPostIU dtoPostIU) {
    Post post = postMapper.toPost(dtoPostIU);
    Post savedPost = postService.savePost(post);
    DtoPost dtoPost = postMapper.toDtoPost(savedPost);
    return ok(dtoPost);
  }

  @Override
  @PutMapping("/{id}")
  public RootEntityResponse<DtoPost> updatePost(@PathVariable Long id, @RequestBody DtoPostIU dtoPostIU) {
    Post post = postService.getPostById(id);
    postMapper.updatePostFromDto(dtoPostIU, post);
    Post savedPost = postService.savePost(post);
    DtoPost dtoPost = postMapper.toDtoPost(savedPost);
    return ok(dtoPost);
  }

  @Override
  @DeleteMapping("/{id}")
  public RootEntityResponse<Boolean> deletePost(@PathVariable Long id) {
    Boolean deleted = postService.deletePost(id);
    if (deleted) {
      return ok(deleted);
    }
    return error("Post not deleted");
  }

  @Override
  @GetMapping("/{id}")
  public RootEntityResponse<DtoPost> getPostById(@PathVariable Long id) {
    Post post = postService.getPostById(id);
    DtoPost dtoPost = postMapper.toDtoPost(post);
    return ok(dtoPost);
  }

  @Override
  @GetMapping("/list")
  public RootEntityResponse<List<DtoPost>> getAllPosts() {
    List<Post> posts = postService.getAllPosts();
    List<DtoPost> dtoPosts = postMapper.toDtoPostListSimple(posts);
    return ok(dtoPosts);
  }

  @Override
  @GetMapping("/list/summary")
  public RootEntityResponse<List<DtoPostSummary>> getAllPostsSummary() {
    List<DtoPostSummary> posts = postService.getAllPostsSummary();
    return ok(posts);
  }

}
