package com.cms.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cms.entity.CallLog;

public interface CallLogRepository extends JpaRepository<CallLog, UUID> {
}
