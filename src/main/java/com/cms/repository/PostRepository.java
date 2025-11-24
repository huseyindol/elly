package com.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.entity.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

}
