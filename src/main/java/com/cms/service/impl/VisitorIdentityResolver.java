package com.cms.service.impl;

import com.cms.config.JwtAuthenticationFilter;
import com.cms.dto.CachedUserDetails;
import com.cms.dto.DtoVisitorIdentity;
import com.cms.exception.UnauthorizedException;
import com.cms.service.IVisitorChatService;
import com.cms.service.IVisitorIdentityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * SecurityContext'ten user bilgisini okuyup VisitorChatService.ensureForTenantUser
 * ile current tenant DB'sinde VisitorIdentity'yi upsert eder.
 */
@Service
@RequiredArgsConstructor
public class VisitorIdentityResolver implements IVisitorIdentityResolver {

  private final IVisitorChatService visitorChatService;

  @Override
  public DtoVisitorIdentity ensureForCurrentUser() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
      throw new UnauthorizedException("Not authenticated");
    }
    if (!(auth.getPrincipal() instanceof JwtAuthenticationFilter.CachedUserPrincipal principal)) {
      throw new UnauthorizedException("Cannot resolve user identity");
    }
    CachedUserDetails cached = principal.getCachedUser();
    Long userId = cached.getId();
    String displayName = cached.getUsername();
    String email = cached.getEmail();
    return visitorChatService.ensureForTenantUser(userId, displayName, email);
  }
}
