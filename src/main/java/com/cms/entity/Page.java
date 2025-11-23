package com.cms.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pages")
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
  private List<Component> components;
}
