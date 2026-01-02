package com.cms.entity;

import java.util.LinkedHashSet;
import java.util.Set;

import com.cms.enums.WidgetTypeEnum;

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
@Table(name = "widgets", indexes = {
    @Index(name = "id_widget_name", columnList = "name"),
    @Index(name = "id_widget_type", columnList = "type"),

    @Index(name = "id_widget_type_status", columnList = "type, status"),
    @Index(name = "id_widget_type_name", columnList = "type, name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Widget extends BaseEntity {
  private String name;
  private String description;
  private WidgetTypeEnum type;
  private String content;
  private Integer orderIndex;
  private Boolean status;

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinTable(name = "widget_banners", joinColumns = @JoinColumn(name = "widget_id"), inverseJoinColumns = @JoinColumn(name = "banner_id"))
  @OrderBy("orderIndex")
  private Set<Banner> banners = new LinkedHashSet<>();

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinTable(name = "widget_posts", joinColumns = @JoinColumn(name = "widget_id"), inverseJoinColumns = @JoinColumn(name = "post_id"))
  @OrderBy("orderIndex")
  private Set<Post> posts = new LinkedHashSet<>();

  @PrePersist
  @PreUpdate
  private void validateWidgetType() {
    if (type == WidgetTypeEnum.BANNER) {
      this.posts = null;
    } else if (type == WidgetTypeEnum.POST) {
      this.banners = null;
    }
  }
}
