package com.cms.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.cms.entity.AiUsageLog;
import com.cms.repository.AiUsageLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AI kullanım loglarını bağımsız transaction (REQUIRES_NEW) içinde kaydeder;
 * böylece ana AI çağrısı hata fırlatsa bile log kaydı rollback olmaz.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiUsageLogWriter {

  private final AiUsageLogRepository aiUsageLogRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public AiUsageLog save(AiUsageLog entry) {
    try {
      return aiUsageLogRepository.save(entry);
    } catch (Exception e) {
      log.warn("[AiUsageLogWriter] AI kullanım logu kaydedilemedi: {}", e.getMessage());
      return entry;
    }
  }
}
