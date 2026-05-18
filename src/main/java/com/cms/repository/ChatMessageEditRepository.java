package com.cms.repository;

import com.cms.entity.ChatMessageEdit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageEditRepository extends JpaRepository<ChatMessageEdit, Long> {

  List<ChatMessageEdit> findByMessageIdOrderByEditedAtDesc(UUID messageId);
}
