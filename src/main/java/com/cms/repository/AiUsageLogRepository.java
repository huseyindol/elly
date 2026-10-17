package com.cms.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cms.entity.AiUsageLog;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

  @Query("""
      SELECT l FROM AiUsageLog l
      WHERE (:action IS NULL OR :action = '' OR l.action = :action)
        AND (:status IS NULL OR :status = '' OR l.status = :status)
        AND (:username IS NULL OR :username = '' OR LOWER(l.username) LIKE LOWER(CONCAT('%', :username, '%')))
        AND (:q IS NULL OR :q = ''
             OR LOWER(COALESCE(l.userInput, '')) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(COALESCE(l.responseText, '')) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(COALESCE(l.endpoint, '')) LIKE LOWER(CONCAT('%', :q, '%'))
             OR LOWER(COALESCE(l.requestId, '')) LIKE LOWER(CONCAT('%', :q, '%')))
      """)
  Page<AiUsageLog> search(
      @Param("action") String action,
      @Param("status") String status,
      @Param("username") String username,
      @Param("q") String q,
      Pageable pageable);

  @Query("""
      SELECT
        COUNT(l),
        SUM(CASE WHEN l.status = 'SUCCESS' THEN 1 ELSE 0 END),
        SUM(CASE WHEN l.status = 'ERROR' THEN 1 ELSE 0 END),
        COALESCE(SUM(l.inputTokens), 0),
        COALESCE(SUM(l.outputTokens), 0),
        COALESCE(SUM(l.thoughtTokens), 0),
        COALESCE(SUM(l.cachedTokens), 0),
        COALESCE(SUM(l.totalTokens), 0),
        COALESCE(AVG(l.durationMs), 0)
      FROM AiUsageLog l
      """)
  List<Object[]> aggregateTotals();

  @Query("""
      SELECT
        l.action,
        MAX(l.endpoint),
        COUNT(l),
        SUM(CASE WHEN l.status = 'SUCCESS' THEN 1 ELSE 0 END),
        SUM(CASE WHEN l.status = 'ERROR' THEN 1 ELSE 0 END),
        COALESCE(SUM(l.inputTokens), 0),
        COALESCE(SUM(l.outputTokens), 0),
        COALESCE(SUM(l.thoughtTokens), 0),
        COALESCE(SUM(l.totalTokens), 0),
        COALESCE(AVG(l.durationMs), 0)
      FROM AiUsageLog l
      GROUP BY l.action
      ORDER BY COALESCE(SUM(l.totalTokens), 0) DESC, COUNT(l) DESC
      """)
  List<Object[]> aggregateByAction();
}
