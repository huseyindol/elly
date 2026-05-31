package com.cms.controller;

import com.cms.dto.DtoNotification;
import com.cms.dto.DtoNotificationReadAllResult;
import com.cms.dto.DtoNotificationUnreadCount;
import com.cms.entity.RootEntityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface INotificationController {

  RootEntityResponse<Page<DtoNotification>> list(Pageable pageable, Boolean unread);

  RootEntityResponse<DtoNotificationUnreadCount> unreadCount();

  RootEntityResponse<DtoNotification> markRead(Long id);

  RootEntityResponse<DtoNotificationReadAllResult> markAllRead();

  RootEntityResponse<Void> delete(Long id);
}
