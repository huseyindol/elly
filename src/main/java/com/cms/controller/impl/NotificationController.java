package com.cms.controller.impl;

import com.cms.controller.INotificationController;
import com.cms.dto.DtoNotification;
import com.cms.dto.DtoNotificationReadAllResult;
import com.cms.dto.DtoNotificationUnreadCount;
import com.cms.entity.RootEntityResponse;
import com.cms.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController extends BaseController implements INotificationController {

  private final INotificationService notificationService;

  @GetMapping
  @Override
  public RootEntityResponse<Page<DtoNotification>> list(
      @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
      @RequestParam(required = false) Boolean unread) {
    return ok(notificationService.list(pageable, unread));
  }

  @GetMapping("/unread-count")
  @Override
  public RootEntityResponse<DtoNotificationUnreadCount> unreadCount() {
    return ok(notificationService.unreadCount());
  }

  @PostMapping("/{id}/read")
  @Override
  public RootEntityResponse<DtoNotification> markRead(@PathVariable Long id) {
    return ok(notificationService.markRead(id));
  }

  @PostMapping("/read-all")
  @Override
  public RootEntityResponse<DtoNotificationReadAllResult> markAllRead() {
    return ok(notificationService.markAllRead());
  }

  @DeleteMapping("/{id}")
  @Override
  public RootEntityResponse<Void> delete(@PathVariable Long id) {
    notificationService.delete(id);
    return ok(null);
  }
}
