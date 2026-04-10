package com.cms.dto;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoUserRoleAssignment {
  @NotEmpty(message = "At least one role ID is required")
  private Set<Long> roleIds;
}
