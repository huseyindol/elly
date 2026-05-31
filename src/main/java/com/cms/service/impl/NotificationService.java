package com.cms.service.impl;

import com.cms.config.JwtAuthenticationFilter;
import com.cms.config.TenantContext;
import com.cms.dto.DtoNotification;
import com.cms.dto.DtoNotificationReadAllResult;
import com.cms.dto.DtoNotificationUnreadCount;
import com.cms.entity.Notification;
import com.cms.enums.NotificationType;
import com.cms.exception.ForbiddenException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.mapper.NotificationMapper;
import com.cms.repository.NotificationRepository;
import com.cms.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements INotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;
  private final NotificationPublisher notificationPublisher;

  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  @Override
  @Transactional(readOnly = true)
  public Page<DtoNotification> list(Pageable pageable, Boolean unreadOnly) {
    Long userId = requireCurrentUserId();
    return runOnBasedb(() -> {
      Page<Notification> page = Boolean.TRUE.equals(unreadOnly)
          ? notificationRepository.findByUserIdAndReadFlagFalseOrderByCreatedAtDesc(userId, pageable)
          : notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
      return page.map(notificationMapper::toDto);
    });
  }

  @Override
  @Transactional(readOnly = true)
  public DtoNotificationUnreadCount unreadCount() {
    Long userId = requireCurrentUserId();
    long count = runOnBasedb(() -> notificationRepository.countByUserIdAndReadFlagFalse(userId));
    return new DtoNotificationUnreadCount(count);
  }

  @Override
  @Transactional
  public DtoNotification markRead(Long id) {
    Long userId = requireCurrentUserId();
    return runOnBasedb(() -> {
      Notification notification = findOwnedNotification(id, userId);
      if (!notification.isReadFlag()) {
        notification.setReadFlag(true);
        notification = notificationRepository.save(notification);
        notificationPublisher.pushUnreadCount(userId);
      }
      return notificationMapper.toDto(notification);
    });
  }

  @Override
  @Transactional
  public DtoNotificationReadAllResult markAllRead() {
    Long userId = requireCurrentUserId();
    return runOnBasedb(() -> {
      int updated = notificationRepository.markAllReadByUserId(userId);
      notificationPublisher.pushUnreadCount(userId);
      return new DtoNotificationReadAllResult(updated);
    });
  }

  @Override
  @Transactional
  public void delete(Long id) {
    Long userId = requireCurrentUserId();
    runOnBasedb(() -> {
      Notification notification = findOwnedNotification(id, userId);
      notificationRepository.delete(notification);
      notificationPublisher.pushUnreadCount(userId);
      return null;
    });
  }

  @Override
  public void notifyUsers(
      Collection<Long> userIds,
      NotificationType type,
      String title,
      String message,
      String link,
      String tenantId,
      Map<String, Object> metadata) {
    if (userIds == null || userIds.isEmpty()) {
      return;
    }
    try {
      Set<Long> uniqueUserIds = new LinkedHashSet<>(userIds);
      for (Long userId : uniqueUserIds) {
        notificationPublisher.createAndPush(userId, type, title, message, link, tenantId, metadata);
      }
    } catch (Exception ex) {
      log.error("Bildirim gonderilemedi: type={}, hata={}", type, ex.getMessage(), ex);
    }
  }

  @Override
  public void notifyAdminPlusUsers(
      NotificationType type,
      String title,
      String message,
      String link,
      String tenantId,
      Map<String, Object> metadata) {
    notifyUsers(notificationPublisher.findAdminPlusUserIds(), type, title, message, link, tenantId, metadata);
  }

  @Override
  public void notifySuperAdminAndAdminUsers(
      NotificationType type,
      String title,
      String message,
      String link,
      String tenantId,
      Map<String, Object> metadata) {
    notifyUsers(notificationPublisher.findSuperAdminAndAdminUserIds(), type, title, message, link, tenantId, metadata);
  }

  private Notification findOwnedNotification(Long id, Long userId) {
    Notification notification = notificationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    if (!notification.getUserId().equals(userId)) {
      throw new ForbiddenException("Bu bildirime erisim yetkiniz yok");
    }
    return notification;
  }

  private Long requireCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof JwtAuthenticationFilter.CachedUserPrincipal principal) {
      return principal.getUserId();
    }
    throw new ForbiddenException("Kimlik dogrulama gerekli");
  }

  private <T> T runOnBasedb(java.util.function.Supplier<T> action) {
    String originalTenant = TenantContext.getTenantId();
    try {
      TenantContext.setTenantId(defaultTenant);
      return action.get();
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }
}
