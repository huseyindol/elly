package com.cms.entity;

import java.util.Date;
import java.util.UUID;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cms_basic_infos", indexes = {
    @Index(name = "idx_cms_basic_info_section_key", columnList = "sectionKey"),
    @Index(name = "idx_cms_basic_info_active", columnList = "isActive")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CmsBasicInfo {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String sectionKey;

  private String title;

  private String description;

  private Boolean isActive;

  private Integer sortOrder;

  @CreatedDate
  private Date createdAt;

  @LastModifiedDate
  private Date updatedAt;
}
