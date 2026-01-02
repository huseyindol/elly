package com.cms.entity;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pages", indexes = {
    @Index(columnList = "slug", name = "id_page_page_slug", unique = true),
    @Index(name = "id_page_status", columnList = "status")
}, uniqueConstraints = { @UniqueConstraint(columnNames = { "slug" }, name = "uc_page_slug") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Page extends BaseEntity {
  private String title;
  private String description;
  private String slug;
  private Boolean status;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "seo_info_id", referencedColumnName = "id")
  private SeoInfo seoInfo;

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinTable(name = "page_components", joinColumns = @JoinColumn(name = "page_id"), inverseJoinColumns = @JoinColumn(name = "component_id"))
  @OrderBy("orderIndex")
  private Set<Component> components = new LinkedHashSet<>();
}
