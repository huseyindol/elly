package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.entity.Permission;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
  Optional<Permission> findByName(String name);

  List<Permission> findByModule(String module);

  boolean existsByName(String name);
}
