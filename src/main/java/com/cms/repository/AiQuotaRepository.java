package com.cms.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cms.entity.AiQuota;

@Repository
public interface AiQuotaRepository extends JpaRepository<AiQuota, String> {}
