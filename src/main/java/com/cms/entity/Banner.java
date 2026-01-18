package com.cms.entity;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "banners", indexes = {
    @Index(name = "id_banner_title", columnList = "title"),
    @Index(name = "id_banner_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Banner extends BaseEntity {
  private String title;
  private String altText;
  @Embedded
  private BannerImage images;
  private String link;
  private String target;
  private String type;
  private Integer orderIndex;
  private Boolean status;
  private String subFolder;
}
