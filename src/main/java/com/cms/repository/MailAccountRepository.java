package com.cms.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.cms.entity.MailAccount;

public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

  Optional<MailAccount> findByIsDefaultTrueAndActiveTrue();

  /** Tüm kayıtların is_default bayrağını temizler; yeni default set edilmeden önce çağrılır. */
  @Modifying
  @Query("UPDATE MailAccount m SET m.isDefault = false WHERE m.isDefault = true")
  void clearAllDefaults();
}
