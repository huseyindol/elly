package com.cms.entity;

import java.util.List;

import com.cms.enums.WidgetTypeEnum;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "widgets")
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
  private List<Banner> banners;

  @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinTable(name = "widget_posts", joinColumns = @JoinColumn(name = "widget_id"), inverseJoinColumns = @JoinColumn(name = "post_id"))
  @OrderBy("orderIndex")
  private List<Post> posts;

  // @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  // @JoinTable(name = "widget_articles", joinColumns = @JoinColumn(name =
  // "widget_id"), inverseJoinColumns = @JoinColumn(name = "article_id"))
  // @OrderBy("orderIndex")
  // private List<Article> articles;

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
