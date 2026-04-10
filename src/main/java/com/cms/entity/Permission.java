package com.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permission_name", columnList = "name", unique = true),
    @Index(name = "idx_permission_module", columnList = "module")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = { "name" }, name = "uc_permission_name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseEntity {

  @Column(name = "name", nullable = false, unique = true)
  private String name; // ör: "posts:create", "pages:read"

  @Column(name = "description")
  private String description;

  @Column(name = "module", nullable = false)
  private String module; // ör: "POSTS", "PAGES", "COMPONENTS"
}
