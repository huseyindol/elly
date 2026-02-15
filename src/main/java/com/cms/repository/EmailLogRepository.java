package com.cms.repository;

import java.util.Date;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.entity.EmailLog;
import com.cms.enums.EmailStatus;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

  List<EmailLog> findByStatusAndCreatedAtBefore(EmailStatus status, Date createdAt, Pageable pageable);

  List<EmailLog> findByStatus(EmailStatus status);
}
