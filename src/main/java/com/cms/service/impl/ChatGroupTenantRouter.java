package com.cms.service.impl;

import com.cms.config.DataSourceConfig;
import com.cms.config.TenantContext;
import com.cms.entity.ChatGroup;
import com.cms.repository.ChatGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Chat gruplarını doğru tenant DB'sinde bulur ve TenantContext'i o DB'ye yönlendirir.
 * AC (basedb) ve TC (tenantX) kayıtlarının karışmasını önler.
 */
@Component
@RequiredArgsConstructor
public class ChatGroupTenantRouter {

  private final ChatGroupRepository groupRepository;
  private final DataSourceConfig.TenantDataSourceProperties tenantProperties;

  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  public Optional<ChatGroup> findGroup(UUID groupId) {
    String original = TenantContext.getTenantId();
    try {
      for (String tenantKey : tenantScanOrder()) {
        setTenantContext(tenantKey);
        Optional<ChatGroup> found = groupRepository.findById(groupId);
        if (found.isPresent()) {
          return found;
        }
      }
      return Optional.empty();
    } finally {
      TenantContext.setTenantId(original);
    }
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
    if (groupTenantId == null || groupTenantId.isBlank()) {
      TenantContext.setTenantId(null);
    } else {
      TenantContext.setTenantId(groupTenantId);
    }
  }

  public void setTenantContext(String tenantKey) {
    if (tenantKey.equals(defaultTenant)) {
      TenantContext.setTenantId(null);
    } else {
      TenantContext.setTenantId(tenantKey);
    }
  }

  /** Tüm tenant DB'lerinde (basedb dahil) sırayla işlem çalıştırır. */
  public <T> List<T> mapAllTenants(TenantScopedCallback<T> callback) {
    String original = TenantContext.getTenantId();
    List<T> results = new ArrayList<>();
    try {
      for (String tenantKey : tenantScanOrder()) {
        setTenantContext(tenantKey);
        results.addAll(callback.run());
      }
      return results;
    } finally {
      TenantContext.setTenantId(original);
    }
  }

  @FunctionalInterface
  public interface TenantScopedCallback<T> {
    List<T> run();
  }

  private List<String> tenantScanOrder() {
    Set<String> ordered = new LinkedHashSet<>();
    if (tenantProperties.getDatasources().containsKey(defaultTenant)) {
      ordered.add(defaultTenant);
    }
    ordered.addAll(tenantProperties.getDatasources().keySet());
    return List.copyOf(ordered);
  }
}
