package com.cms.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.cms.config.RabbitMQConfig;
import com.cms.entity.EmailLog;
import com.cms.enums.EmailStatus;
import com.cms.repository.EmailLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRescueJob {

  private final EmailLogRepository emailLogRepository;
  private final RabbitTemplate rabbitTemplate;

  private static final int BATCH_SIZE = 50;
  private static final long FIVE_MINUTES_MS = 5 * 60 * 1000L;

  /**
   * 5 dakikada bir çalışır.
   * PENDING durumda olan ve 5 dakikadan eski mailleri bulur
   * ve tekrar RabbitMQ kuyruğuna gönderir.
   */
  @Scheduled(fixedRate = 300000) // 5 dakika = 300000ms
  public void rescueStuckEmails() {
    Date fiveMinutesAgo = new Date(System.currentTimeMillis() - FIVE_MINUTES_MS);

    List<EmailLog> stuckEmails = emailLogRepository.findByStatusAndCreatedAtBefore(
        EmailStatus.PENDING,
        fiveMinutesAgo,
        PageRequest.of(0, BATCH_SIZE));

    if (stuckEmails.isEmpty()) {
      return;
    }

    log.info("EmailRescueJob: Found {} stuck PENDING emails, re-queuing...", stuckEmails.size());

    for (EmailLog emailLog : stuckEmails) {
      try {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.EMAIL_EXCHANGE,
            RabbitMQConfig.EMAIL_ROUTING_KEY,
            emailLog.getId());
        log.debug("Re-queued email ID: {}", emailLog.getId());
      } catch (Exception e) {
        log.error("Failed to re-queue email ID: {}: {}", emailLog.getId(), e.getMessage());
      }
    }

    log.info("EmailRescueJob: Re-queued {} stuck emails", stuckEmails.size());
  }
}
