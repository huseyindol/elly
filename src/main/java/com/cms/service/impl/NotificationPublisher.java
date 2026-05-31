package com.cms.service.impl;

import com.cms.config.TenantContext;
import com.cms.dto.DtoNotification;
import com.cms.dto.DtoNotificationUnreadCount;
import com.cms.entity.Notification;
import com.cms.entity.User;
import com.cms.enums.NotificationType;
import com.cms.mapper.NotificationMapper;
import com.cms.repository.NotificationRepository;
import com.cms.repository.UserRepository;
import com.cms.service.INotificationPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationPublisher implements INotificationPublisher {

  private static final Set<String> ADMIN_PLUS_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "EDITOR");
  private static final Set<String> SUPER_ADMIN_ADMIN_ROLES = Set.of("SUPER_ADMIN", "ADMIN");

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;
  private final UserRepository userRepository;
  private final SimpMessagingTemplate messagingTemplate;

  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void createAndPush(
      Long userId,
      NotificationType type,
      String title,
      String message,
      String link,
      String tenantId,
      Map<String, Object> metadata) {
    runOnBasedb(() -> {
      Notification notification = Notification.builder()
          .userId(userId)
          .type(type)
          .title(sanitizeText(title))
          .message(sanitizeText(message))
          .link(link)
          .tenantId(tenantId)
          .metadata(metadata)
          .readFlag(false)
          .build();
      Notification saved = notificationRepository.save(notification);
      DtoNotification dto = notificationMapper.toDto(saved);
      pushNotification(userId, dto);
      pushUnreadCount(userId);
      return null;
    });
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public void pushUnreadCount(Long userId) {
    runOnBasedb(() -> {
      long count = notificationRepository.countByUserIdAndReadFlagFalse(userId);
      DtoNotificationUnreadCount payload = new DtoNotificationUnreadCount(count);
      userRepository.findById(userId).ifPresent(user ->
          messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications/unread-count", payload));
      return null;
    });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
  public List<Long> findUserIdsByRoles(Set<String> roleNames) {
    return runOnBasedb(() -> userRepository.findAll().stream()
        .filter(User::getIsActive)
        .filter(user -> user.getRoles().stream().anyMatch(role -> roleNames.contains(role.getName())))
        .map(User::getId)
        .toList());
  }

  public List<Long> findAdminPlusUserIds() {
    return findUserIdsByRoles(ADMIN_PLUS_ROLES);
  }

  public List<Long> findSuperAdminAndAdminUserIds() {
    return findUserIdsByRoles(SUPER_ADMIN_ADMIN_ROLES);
  }

  private void pushNotification(Long userId, DtoNotification dto) {
    userRepository.findById(userId).ifPresentOrElse(user -> {
      messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", dto);
      log.debug("Notification WS push → userId={} username={} notificationId={}",
          userId, user.getUsername(), dto.getId());
    }, () -> log.warn("Notification WS push skipped — userId={} not found in basedb", userId));
  }

  private String sanitizeText(String value) {
    if (value == null) {
      return "";
    }
    return Jsoup.clean(value, Safelist.none()).trim();
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
