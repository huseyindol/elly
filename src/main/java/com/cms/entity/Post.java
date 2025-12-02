package com.cms.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "posts", indexes = {
    @Index(name = "idx_post_slug", columnList = "slug", unique = true),
    @Index(name = "idx_post_status", columnList = "status"),
    @Index(name = "idx_post_seo_info_id", columnList = "seo_info_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = { "slug" }, name = "uc_post_slug")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Post extends BaseEntity {
  private String title;
  private String content;
  @Column(name = "slug", nullable = false, unique = true)
  private String slug;
  private Integer orderIndex;
  private Boolean status;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "seo_info_id", referencedColumnName = "id")
  private SeoInfo seoInfo;

}
