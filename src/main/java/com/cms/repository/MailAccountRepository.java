package com.cms.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.entity.MailAccount;

public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {

  /** Aktif olan tum mail hesaplari — form yaratirken secime sunulur. */
  List<MailAccount> findAllByActiveTrue();
}
