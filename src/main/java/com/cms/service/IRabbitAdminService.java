package com.cms.service;

import java.util.List;

import com.cms.dto.rabbit.DtoRabbitMessage;
import com.cms.dto.rabbit.DtoRabbitOverview;
import com.cms.dto.rabbit.DtoRabbitQueue;

/**
 * RabbitMQ broker yonetimi icin panel-facing servis arayuzu.
 * CMS, RabbitMQ Management HTTP API'sini proxy'ler ve JWT + @PreAuthorize
 * ile korur — panel dogrudan :15672'ye erisemez.
 */
public interface IRabbitAdminService {

  DtoRabbitOverview getOverview();

  List<DtoRabbitQueue> listQueues();

  DtoRabbitQueue getQueue(String name);

  /**
   * Kuyruktaki ilk {@code count} mesaji peek eder (ack_requeue_true — queue'dan silmez).
   */
  List<DtoRabbitMessage> peekMessages(String queueName, int count);

  /**
   * Kuyruktaki TUM mesajlari siler. Irreversible.
   */
  void purgeQueue(String queueName);

  /**
   * Bir payload'i hedef queue'ya yeniden publish eder (default exchange uzerinden,
   * routingKey = queue adi). DLQ'dan ana kuyruga geri atmak icin kullanilir.
   */
  void republishMessage(String sourceQueue, String targetQueue, String payload, String contentType);
}
