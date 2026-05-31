package com.cms.service;

import com.cms.dto.DtoNotification;
import com.cms.dto.DtoNotificationReadAllResult;
import com.cms.dto.DtoNotificationUnreadCount;
import com.cms.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Map;

public interface INotificationService {

  Page<DtoNotification> list(Pageable pageable, Boolean unreadOnly);

  DtoNotificationUnreadCount unreadCount();

  DtoNotification markRead(Long id);

  DtoNotificationReadAllResult markAllRead();

  void delete(Long id);

  /**
   * Belirtilen kullanıcılara bildirim yazar ve WebSocket ile push eder.
   * Hata olursa loglanır; çağıran akışı etkilemez.
   */
  void notifyUsers(
      Collection<Long> userIds,
      NotificationType type,
      String title,
      String message,
      String link,
      String tenantId,
      Map<String, Object> metadata);

  /** ADMIN+ (EDITOR, ADMIN, SUPER_ADMIN) kullanıcılara bildirim gönderir. */
  void notifyAdminPlusUsers(
      NotificationType type,
      String title,
      String message,
      String link,
      String tenantId,
      Map<String, Object> metadata);

  /** SUPER_ADMIN ve ADMIN kullanıcılara bildirim gönderir. */
  void notifySuperAdminAndAdminUsers(
      NotificationType type,
      String title,
      String message,
      String link,
      String tenantId,
      Map<String, Object> metadata);
}
