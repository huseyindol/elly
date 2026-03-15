package com.cms.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoCacheInfo {

  private String tenantId;
  private String name;
  private long keyCount;
  private String stats;
  private List<String> keys;

}
