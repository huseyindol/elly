package com.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "assets", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "subFolder" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Assets extends BaseEntity {
  @Column(unique = false)
  private String name;
  private String path;
  private String type;
  private String extension;
  private String subFolder;
}
