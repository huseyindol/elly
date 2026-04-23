package com.cms.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.entity.EmailTemplate;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

  Optional<EmailTemplate> findByTenantIdAndTemplateKey(String tenantId, String templateKey);

  Optional<EmailTemplate> findByTenantIdIsNullAndTemplateKey(String templateKey);

  boolean existsByTenantIdAndTemplateKey(String tenantId, String templateKey);

  boolean existsByTenantIdIsNullAndTemplateKey(String templateKey);

  /**
   * Panel listesi: mevcut tenant'a özgü + global (tenantId=null) template'leri döner.
   */
  Page<EmailTemplate> findByTenantIdOrTenantIdIsNull(String tenantId, Pageable pageable);
}
