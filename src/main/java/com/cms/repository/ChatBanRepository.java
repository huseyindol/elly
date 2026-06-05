package com.cms.repository;

import com.cms.entity.ChatBan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatBanRepository extends JpaRepository<ChatBan, UUID> {

  boolean existsByGroupIdAndSessionId(UUID groupId, UUID sessionId);

  boolean existsByGroupIdAndVisitorId(UUID groupId, Long visitorId);

  Optional<ChatBan> findByGroupIdAndSessionId(UUID groupId, UUID sessionId);

  Optional<ChatBan> findByGroupIdAndVisitorId(UUID groupId, Long visitorId);

  List<ChatBan> findByGroupIdOrderByCreatedAtDesc(UUID groupId);
}
