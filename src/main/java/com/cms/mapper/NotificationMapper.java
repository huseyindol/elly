package com.cms.mapper;

import com.cms.dto.DtoNotification;
import com.cms.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(target = "read", source = "readFlag")
  DtoNotification toDto(Notification entity);
}
