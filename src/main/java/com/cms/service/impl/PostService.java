package com.cms.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DtoPostSummary;
import com.cms.entity.Post;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.PostRepository;
import com.cms.service.IPostService;

@Service
public class PostService implements IPostService {

  @Autowired
  private PostRepository postRepository;

  @Override
  @Transactional
  public Post savePost(Post post) {
    return postRepository.save(post);
  }

  @Override
  public Post getPostById(Long id) {
    return postRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Post", id));
  }

  @Override
  @Transactional
  public Boolean deletePost(Long id) {
    if (postRepository.existsById(id)) {
      postRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  public List<Post> getAllPosts() {
    return postRepository.findAllWithRelations();
  }

  @Override
  public List<DtoPostSummary> getAllPostsSummary() {
    return postRepository.findAllWithSummary();
  }

}
