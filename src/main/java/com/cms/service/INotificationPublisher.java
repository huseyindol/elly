package com.cms.service;

import com.cms.dto.DtoNotification;
import com.cms.enums.NotificationType;

import java.util.Map;

/** Bildirim yazma + WebSocket push — REQUIRES_NEW transaction ile basedb'ye yazar. */
public interface INotificationPublisher {

  void createAndPush(
      Long userId,
      NotificationType type,
      String title,
      String message,
      String link,
      String tenantId,
      Map<String, Object> metadata);

  void pushUnreadCount(Long userId);
}
