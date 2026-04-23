package com.cms.controller;

import java.util.List;

import com.cms.dto.rabbit.DtoRabbitMessage;
import com.cms.dto.rabbit.DtoRabbitOverview;
import com.cms.dto.rabbit.DtoRabbitQueue;
import com.cms.dto.rabbit.RepublishRequest;
import com.cms.entity.RootEntityResponse;

public interface IRabbitAdminController {

  RootEntityResponse<DtoRabbitOverview> getOverview();

  RootEntityResponse<List<DtoRabbitQueue>> listQueues();

  RootEntityResponse<DtoRabbitQueue> getQueue(String name);

  RootEntityResponse<List<DtoRabbitMessage>> peekMessages(String name, int count);

  RootEntityResponse<Void> purgeQueue(String name);

  RootEntityResponse<Void> republish(String name, RepublishRequest request);
}
