package com.cms.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.entity.EmailLog;
import com.cms.enums.EmailStatus;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

  List<EmailLog> findByStatusAndCreatedAtBefore(EmailStatus status, Date createdAt, Pageable pageable);

  List<EmailLog> findByStatus(EmailStatus status);

  /**
   * RabbitMQ consumer icin: mailAccount lazy proxy'sini ayni sorguda initialize
   * eder. Consumer thread'inde Hibernate session kisa surede kapandigindan
   * {@code emailLog.getMailAccount()} cagrisinda LazyInitializationException
   * firlatmamak icin bu metot kullanilmalidir.
   */
  @Query("SELECT el FROM EmailLog el LEFT JOIN FETCH el.mailAccount WHERE el.id = :id")
  Optional<EmailLog> findByIdWithMailAccount(@Param("id") Long id);
}
