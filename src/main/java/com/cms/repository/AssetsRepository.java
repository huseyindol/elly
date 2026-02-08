package com.cms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.cms.entity.Assets;

public interface AssetsRepository extends JpaRepository<Assets, Long> {

  Optional<Assets> findByPath(String path);

  Optional<Assets> findByName(String name);

  List<Assets> findAllByName(String name);

  List<Assets> findBySubFolder(String subFolder);

  Page<Assets> findBySubFolder(String subFolder, Pageable pageable);

  Optional<Assets> findByNameAndSubFolder(String name, String subFolder);

  List<Assets> findByNameContainingIgnoreCase(String name);

  Page<Assets> findByNameContainingIgnoreCase(String name, Pageable pageable);

  Page<Assets> findBySubFolderAndNameContainingIgnoreCase(String subFolder, String name, Pageable pageable);

  @Query("SELECT DISTINCT a.subFolder FROM Assets a WHERE a.subFolder IS NOT NULL AND a.subFolder != ''")
  List<String> findDistinctSubFolders();

}
