package com.cms.repository;

import com.cms.entity.VisitorIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Tenant DB'sinde visitor kimliklerini yönetir.
 * <p>NOT: Bu repository her zaman TenantContext'in işaret ettiği tenant DB'sinde çalışır.
 */
public interface VisitorIdentityRepository extends JpaRepository<VisitorIdentity, Long> {

  /** Kayıtlı tenant user'ı için kimlik kaydı. */
  Optional<VisitorIdentity> findByTenantUserId(Long tenantUserId);

  /** Anonim ziyaretçi cookie token'ı için kayıt. */
  Optional<VisitorIdentity> findBySessionToken(UUID sessionToken);
}
