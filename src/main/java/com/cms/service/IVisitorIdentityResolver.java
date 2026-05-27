package com.cms.service;

import com.cms.dto.DtoVisitorIdentity;

/**
 * Mevcut authenticated user için VisitorIdentity'yi çözer/yaratır.
 * <p>Tenant Chat (TC) akışında frontend her ekran açılışında session endpoint'ini
 * çağırarak bu kimliği elde eder.
 */
public interface IVisitorIdentityResolver {

  /**
   * Current SecurityContext'teki user için bu tenant'a ait VisitorIdentity'yi getirir;
   * yoksa yaratır.
   * @throws com.cms.exception.UnauthorizedException Auth yoksa
   */
  DtoVisitorIdentity ensureForCurrentUser();
}
