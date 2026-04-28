package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DtoUserResponse {
  private Long id;
  private String username;
  private String email;
  private String firstName;
  private String lastName;
  private String provider;
  private Boolean isActive;
  private List<String> managedTenants;
  private List<String> roles;
  private Date createdAt;
}

