package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.entity.MailAccount;

public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

  /** Aktif olan tum mail hesaplari — form yaratirken secime sunulur. */
  List<MailAccount> findAllByActiveTrue();

  /** Belirli bir tenant'a ait tüm hesaplar. */
  List<MailAccount> findAllByTenantId(String tenantId);

  /** Belirli bir tenant'a ait aktif hesaplar. */
  List<MailAccount> findAllByTenantIdAndActiveTrue(String tenantId);

  /** Doğrulama e-postası için: tenant'ın ilk aktif hesabı. */
  java.util.Optional<MailAccount> findFirstByTenantIdAndActiveTrueOrderByIdAsc(String tenantId);
}
