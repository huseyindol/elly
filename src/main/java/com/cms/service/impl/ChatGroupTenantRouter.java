package com.cms.service.impl;

import com.cms.config.TenantContext;
import com.cms.entity.ChatGroup;
import com.cms.repository.ChatGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Chat gruplarını MEVCUT tenant context'inde bulur/yönlendirir.
 *
 * <p>basedb/AC ayrımı YOKTUR — her grup ait olduğu tenant DB'sinde yaşar. Context;
 * filtre (JWT tenantId), WS CONNECT (token tenantId) veya WS destination
 * ({@code /app/tenant-chat/{tid}/..}) tarafından zaten doğru set edilir; bu yüzden
 * eski cross-tenant tarama kaldırıldı.
 */
@Component
@RequiredArgsConstructor
public class ChatGroupTenantRouter {

  private final ChatGroupRepository groupRepository;

  public Optional<ChatGroup> findGroup(UUID groupId) {
    return groupRepository.findById(groupId);
  }

  public ChatGroup requireGroup(UUID groupId) {
    return findGroup(groupId).orElseThrow(() ->
        new com.cms.exception.ResourceNotFoundException("Chat group not found: " + groupId));
  }

  /** Grubun bulunduğu DB'ye TenantContext'i set eder. */
  public void useGroupDatabase(ChatGroup group) {
    setTenantContextForGroup(group.getTenantId());
  }

  public void setTenantContextForGroup(String groupTenantId) {
    TenantContext.setTenantId(groupTenantId);
  }

  public void setTenantContext(String tenantKey) {
    TenantContext.setTenantId(tenantKey);
  }
}
