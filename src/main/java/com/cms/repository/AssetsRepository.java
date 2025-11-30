package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.entity.Assets;

public interface AssetsRepository extends JpaRepository<Assets, Long> {

  Optional<Assets> findByPath(String path);

  Optional<Assets> findByName(String name);

}
