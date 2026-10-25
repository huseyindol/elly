package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoIceServer;
import com.cms.dto.DtoNotification;
import com.cms.dto.DtoNotificationReadAllResult;
import com.cms.dto.DtoNotificationUnreadCount;
import com.cms.dto.DtoPushDeviceRequest;
import com.cms.dto.DtoPushDeviceResponse;
import com.cms.entity.RootEntityResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface INotificationController {

  RootEntityResponse<Page<DtoNotification>> list(Pageable pageable, Boolean unread);

  RootEntityResponse<DtoNotificationUnreadCount> unreadCount();

  RootEntityResponse<DtoNotification> markRead(Long id);

  RootEntityResponse<DtoNotificationReadAllResult> markAllRead();

  RootEntityResponse<Void> delete(Long id);

  RootEntityResponse<DtoPushDeviceResponse> registerDevice(DtoPushDeviceRequest req);

  RootEntityResponse<List<DtoPushDeviceResponse>> listDevices();

  RootEntityResponse<Boolean> unregisterDevice(String token);

  RootEntityResponse<List<DtoIceServer>> getIceServers();
}
