package com.cms.entity;

import java.util.LinkedHashSet;
import java.util.Set;

import com.cms.enums.ComponentTypeEnum;

import org.hibernate.annotations.BatchSize;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "components", indexes = {
    @Index(name = "id_component_name", columnList = "name"),
    @Index(name = "id_component_type", columnList = "type"),

    @Index(name = "id_component_type_status", columnList = "type, status"),
    @Index(name = "id_component_type_name", columnList = "type, name")
})
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
  private Set<Page> pages = new LinkedHashSet<>();

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinTable(name = "component_banners", joinColumns = @JoinColumn(name = "component_id"), inverseJoinColumns = @JoinColumn(name = "banner_id"))
  @OrderBy("orderIndex")
  @BatchSize(size = 20)
  private Set<Banner> banners = new LinkedHashSet<>();

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinTable(name = "component_widgets", joinColumns = @JoinColumn(name = "component_id"), inverseJoinColumns = @JoinColumn(name = "widget_id"))
  @OrderBy("orderIndex")
  @BatchSize(size = 20)
  private Set<Widget> widgets = new LinkedHashSet<>();

  @PrePersist
  @PreUpdate
  private void validateComponentType() {
    if (type == ComponentTypeEnum.BANNER) {
      this.widgets = null;
    } else if (type == ComponentTypeEnum.WIDGET) {
      this.banners = null;
    }
  }
}
