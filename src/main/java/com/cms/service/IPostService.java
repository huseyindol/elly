package com.cms.service;

import java.util.List;

import com.cms.entity.Post;

public interface IPostService {
  Post savePost(Post post);

  Post getPostById(Long id);

  Boolean deletePost(Long id);

  List<Post> getAllPosts();
}
