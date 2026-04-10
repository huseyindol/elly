package com.cms.dto;

import java.io.Serializable;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Redis cache'e yazılacak hafif kullanıcı bilgisi.
 * JPA entity yerine bu DTO cache'lenir — Hibernate proxy sorunlarından kaçınır.
 * Her istekte DB'ye gitmek yerine Redis'ten okunur.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CachedUserDetails implements Serializable {
  private static final long serialVersionUID = 1L;

  private Long id;
  private String username;
  private String password;
  private String email;
  private Boolean isActive;
  private Long tokenVersion;
  private Set<String> authorities; // "ROLE_SUPER_ADMIN", "posts:create", vb.
}
