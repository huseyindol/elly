package com.cms.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoRole {
  private Long id;
  private String name;
  private String description;
  private Set<DtoPermission> permissions;
}
