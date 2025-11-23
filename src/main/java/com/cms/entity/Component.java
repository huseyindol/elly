package com.cms.entity;

import java.util.List;

import com.cms.enums.ComponentTypeEnum;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "components")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Component extends BaseEntity {
  private String name;
  private String description;
  private ComponentTypeEnum type;
  private String content;
  private Integer orderIndex;
  private Boolean status;

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinTable(name = "page_components", joinColumns = @JoinColumn(name = "component_id"), inverseJoinColumns = @JoinColumn(name = "page_id"))
  private List<Page> pages;

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinTable(name = "component_banners", joinColumns = @JoinColumn(name = "component_id"), inverseJoinColumns = @JoinColumn(name = "banner_id"))
  @OrderBy("orderIndex")
  private List<Banner> banners;

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinTable(name = "component_widgets", joinColumns = @JoinColumn(name = "component_id"), inverseJoinColumns = @JoinColumn(name = "widget_id"))
  @OrderBy("orderIndex")
  private List<Widget> widgets;
}
