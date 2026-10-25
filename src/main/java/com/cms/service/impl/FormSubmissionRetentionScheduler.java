package com.cms.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cms.config.DataSourceConfig;
import com.cms.config.TenantContext;
import com.cms.service.IFormSubmissionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FormSubmissionRetentionScheduler {

  private final DataSourceConfig.TenantDataSourceProperties tenantProperties;
  private final IFormSubmissionService formSubmissionService;

  @Value("${app.forms.retention-days:365}")
  private int retentionDays;

  @Scheduled(cron = "${app.forms.retention-cron:0 30 3 * * *}")
  public void cleanupExpiredFormSubmissions() {
    if (retentionDays <= 0 || tenantProperties == null || tenantProperties.getDatasources() == null) {
      return;
    }
    tenantProperties.getDatasources().keySet().forEach(tenantId -> {
      try {
        TenantContext.setTenantId(tenantId);
        int deleted = formSubmissionService.cleanupAllExpiredSubmissions(retentionDays);
        if (deleted > 0) {
          log.info("KVKK/GDPR form yaniti temizligi: tenant={}, retentionDays={}, silinen={}",
              tenantId, retentionDays, deleted);
        }
      } catch (Exception ex) {
        log.warn("KVKK/GDPR form yaniti temizligi hatasi: tenant={}, hata={}", tenantId, ex.getMessage());
      } finally {
        TenantContext.clear();
      }
    });
  }
}
