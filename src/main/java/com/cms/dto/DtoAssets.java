package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoAssets {
  private Long id;
  private String path;
  private String type;
  private String name;
  private String extension;
  private String subFolder;
}
