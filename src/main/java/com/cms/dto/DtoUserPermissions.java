package com.cms.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Kullanıcının sahip olduğu roller ve izinleri döndüren DTO.
 * Panel (Next.js) tarafında PermissionGate/usePermission gibi yapılarla kullanılır.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DtoUserPermissions {
  /** Kullanıcının rolleri: ["SUPER_ADMIN", "ADMIN", ...] */
  private List<String> roles;

  /** Düz liste: ["posts:create", "posts:read", "pages:read", ...] */
  private List<String> permissions;

  /** Modüle göre gruplu: {"POSTS": ["create","read","update","delete"], "PAGES": [...]} */
  private Map<String, List<String>> permissionsByModule;
}
