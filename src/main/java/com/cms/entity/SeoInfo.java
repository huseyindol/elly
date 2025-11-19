package com.cms.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seo_infos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeoInfo extends BaseEntity {
  private String title;
  private String description;
  private String keywords;
  private String canonicalUrl;
  private Boolean noIndex;
  private Boolean noFollow;
}
