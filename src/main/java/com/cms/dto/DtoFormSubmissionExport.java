package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DtoFormSubmissionExport {

  private String fileName;
  private int count;
  private String csvContent;
}
