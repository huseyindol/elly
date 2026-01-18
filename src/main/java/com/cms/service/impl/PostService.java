package com.cms.service.impl;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DtoPostSummary;
import com.cms.entity.Post;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.PostRepository;
import com.cms.service.IPostService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostService implements IPostService {

  private final PostRepository postRepository;

  @Override
  @Transactional
  @CacheEvict(value = "posts", allEntries = true)
  public Post savePost(Post post) {
    return postRepository.save(post);
  }

  @Override
  @Cacheable(value = "posts", key = "#id")
  public Post getPostById(Long id) {
    return postRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Post", id));
  }

  @Override
  @Transactional
  @CacheEvict(value = "posts", allEntries = true)
  public Boolean deletePost(Long id) {
    if (postRepository.existsById(id)) {
      postRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  @Cacheable(value = "posts", key = "'getAllPosts'")
  public List<Post> getAllPosts() {
    return postRepository.findAllWithRelations();
  }

  @Override
  @Cacheable(value = "posts", key = "'getAllPostsSummary'")
  public List<DtoPostSummary> getAllPostsSummary() {
    return postRepository.findAllWithSummary();
  }

  @Override
  @Cacheable(value = "posts", key = "'slug_' + #slug")
  public Post getPostBySlug(String slug) {
    return postRepository.findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Post not found with slug: " + slug));
  }
}
