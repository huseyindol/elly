package com.cms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ratings", indexes = {
    @Index(name = "idx_rating_post_id", columnList = "post_id"),
    @Index(name = "idx_rating_user_post", columnList = "user_identifier, post_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = { "user_identifier", "post_id" }, name = "uc_rating_user_post")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Rating extends BaseEntity {

  @JsonIgnore // Prevents circular reference during Redis serialization
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", nullable = false)
  private Post post;

  @Column(nullable = false)
  private Integer rating; // 1-5 arası puan

  @Column(name = "user_identifier", nullable = false)
  private String userIdentifier; // IP adresi veya user ID

  @Column(length = 500)
  private String comment; // Opsiyonel değerlendirme yorumu
}
