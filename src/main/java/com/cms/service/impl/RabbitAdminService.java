package com.cms.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.cms.dto.rabbit.DtoRabbitMessage;
import com.cms.dto.rabbit.DtoRabbitOverview;
import com.cms.dto.rabbit.DtoRabbitQueue;
import com.cms.exception.BaseException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.ValidationException;
import com.cms.service.IRabbitAdminService;

import lombok.extern.slf4j.Slf4j;

/**
 * RabbitMQ Management HTTP API istemcisi. Panel admin UI calisan kullanicinin
 * JWT-authenticated cagrilarini 15672'deki management plugin'e forward eder.
 *
 * Management API referansi: https://www.rabbitmq.com/management.html#http-api
 */
@Slf4j
@Service
public class RabbitAdminService implements IRabbitAdminService {

  private final RestClient restClient;
  private final String vhost;
  private final String encodedVhost;

  public RabbitAdminService(
      @Qualifier("rabbitMgmtRestClient") RestClient restClient,
      @Value("${rabbitmq.mgmt.vhost:/}") String vhost) {
    this.restClient = restClient;
    this.vhost = vhost;
    // RabbitMQ Management API'de vhost "/" -> "%2F" olarak encode edilir
    this.encodedVhost = URLEncoder.encode(vhost, StandardCharsets.UTF_8);
  }

  // ==================== OVERVIEW ====================

  @Override
  public DtoRabbitOverview getOverview() {
    try {
      Map<String, Object> raw = restClient.get()
          .uri("/overview")
          .retrieve()
          .body(new ParameterizedTypeReference<Map<String, Object>>() {});

      if (raw == null) {
        throw new BrokerUnavailableException("RabbitMQ management /overview bos response dondu");
      }

      Map<String, Object> objectTotals = asMap(raw.get("object_totals"));
      Map<String, Object> messageStats = asMap(raw.get("queue_totals"));

      return DtoRabbitOverview.builder()
          .rabbitmqVersion(asString(raw.get("rabbitmq_version")))
          .erlangVersion(asString(raw.get("erlang_version")))
          .clusterName(asString(raw.get("cluster_name")))
          .totalMessages(asLong(messageStats.get("messages")))
          .totalConsumers(asLong(objectTotals.get("consumers")))
          .queueCount(asInt(objectTotals.get("queues")))
          .exchangeCount(asInt(objectTotals.get("exchanges")))
          .connectionCount(asInt(objectTotals.get("connections")))
          .channelCount(asInt(objectTotals.get("channels")))
          .build();

    } catch (ResourceAccessException | HttpServerErrorException e) {
      throw new BrokerUnavailableException("RabbitMQ management servisine ulasilamadi: " + e.getMessage(), e);
    }
  }

  // ==================== QUEUES ====================

  @Override
  public List<DtoRabbitQueue> listQueues() {
    try {
      List<Map<String, Object>> raw = restClient.get()
          .uri("/queues/" + encodedVhost)
          .retrieve()
          .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

      if (raw == null || raw.isEmpty()) {
        return List.of();
      }

      List<DtoRabbitQueue> result = new ArrayList<>(raw.size());
      for (Map<String, Object> q : raw) {
        result.add(mapQueue(q));
      }
      return result;

    } catch (ResourceAccessException | HttpServerErrorException e) {
      throw new BrokerUnavailableException("RabbitMQ queue listesi alinamadi: " + e.getMessage(), e);
    }
  }

  @Override
  public DtoRabbitQueue getQueue(String name) {
    validateName(name, "queue");
    try {
      Map<String, Object> raw = restClient.get()
          .uri("/queues/" + encodedVhost + "/" + name)
          .retrieve()
          .body(new ParameterizedTypeReference<Map<String, Object>>() {});
      if (raw == null) {
        throw new ResourceNotFoundException("Queue '" + name + "' bulunamadi");
      }
      return mapQueue(raw);

    } catch (HttpClientErrorException.NotFound e) {
      throw new ResourceNotFoundException("Queue '" + name + "' bulunamadi");
    } catch (ResourceAccessException | HttpServerErrorException e) {
      throw new BrokerUnavailableException("RabbitMQ queue detayi alinamadi: " + e.getMessage(), e);
    }
  }

  // ==================== MESSAGES ====================

  @Override
  public List<DtoRabbitMessage> peekMessages(String queueName, int count) {
    validateName(queueName, "queue");
    if (count <= 0 || count > 100) {
      throw new ValidationException("count 1 ile 100 arasinda olmalidir");
    }

    Map<String, Object> body = new HashMap<>();
    body.put("count", count);
    // ack_requeue_true -> mesaj kuyrukta kalir, peek davranisi
    body.put("ackmode", "ack_requeue_true");
    body.put("encoding", "auto");
    body.put("truncate", 50_000);

    try {
      List<Map<String, Object>> raw = restClient.post()
          .uri("/queues/" + encodedVhost + "/" + queueName + "/get")
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

      if (raw == null || raw.isEmpty()) {
        return List.of();
      }

      List<DtoRabbitMessage> result = new ArrayList<>(raw.size());
      for (Map<String, Object> m : raw) {
        result.add(DtoRabbitMessage.builder()
            .payload(asString(m.get("payload")))
            .payloadEncoding(asString(m.get("payload_encoding")))
            .properties(asMap(m.get("properties")))
            .messageCount(asLong(m.get("message_count")))
            .routingKey(asString(m.get("routing_key")))
            .redelivered(asBool(m.get("redelivered")))
            .exchange(asString(m.get("exchange")))
            .build());
      }
      return result;

    } catch (HttpClientErrorException.NotFound e) {
      throw new ResourceNotFoundException("Queue '" + queueName + "' bulunamadi");
    } catch (ResourceAccessException | HttpServerErrorException e) {
      throw new BrokerUnavailableException("RabbitMQ queue mesajlari okunamadi: " + e.getMessage(), e);
    }
  }

  // ==================== DESTRUCTIVE ====================

  @Override
  public void purgeQueue(String queueName) {
    validateName(queueName, "queue");
    try {
      restClient.delete()
          .uri("/queues/" + encodedVhost + "/" + queueName + "/contents")
          .retrieve()
          .toBodilessEntity();
      log.warn("RabbitMQ queue purged: vhost={}, name={}", vhost, queueName);

    } catch (HttpClientErrorException.NotFound e) {
      throw new ResourceNotFoundException("Queue '" + queueName + "' bulunamadi");
    } catch (ResourceAccessException | HttpServerErrorException e) {
      throw new BrokerUnavailableException("RabbitMQ queue purge edilemedi: " + e.getMessage(), e);
    }
  }

  @Override
  public void republishMessage(String sourceQueue, String targetQueue, String payload, String contentType) {
    validateName(sourceQueue, "sourceQueue");
    validateName(targetQueue, "targetQueue");
    if (payload == null || payload.isBlank()) {
      throw new ValidationException("payload bos olamaz");
    }

    // Default exchange ("") uzerinden publish — routingKey = queue adi olunca mesaj o queue'ya duser.
    Map<String, Object> body = new HashMap<>();
    body.put("properties", Map.of("content_type",
        (contentType == null || contentType.isBlank()) ? "application/json" : contentType));
    body.put("routing_key", targetQueue);
    body.put("payload", payload);
    body.put("payload_encoding", "string");

    try {
      Map<String, Object> response = restClient.post()
          .uri("/exchanges/" + encodedVhost + "/amq.default/publish")
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(new ParameterizedTypeReference<Map<String, Object>>() {});

      Boolean routed = response == null ? null : asBool(response.get("routed"));
      if (!Boolean.TRUE.equals(routed)) {
        throw new ValidationException("Mesaj publish edildi ama hedef queue'ya route edilemedi (targetQueue='" + targetQueue + "' var mi?)");
      }
      log.warn("RabbitMQ message republished: source={}, target={}, bytes={}",
          sourceQueue, targetQueue, payload.length());

    } catch (HttpClientErrorException.NotFound e) {
      throw new ResourceNotFoundException("Hedef exchange veya queue bulunamadi");
    } catch (ResourceAccessException | HttpServerErrorException e) {
      throw new BrokerUnavailableException("RabbitMQ publish basarisiz: " + e.getMessage(), e);
    }
  }

  // ==================== HELPERS ====================

  private DtoRabbitQueue mapQueue(Map<String, Object> q) {
    return DtoRabbitQueue.builder()
        .name(asString(q.get("name")))
        .vhost(asString(q.get("vhost")))
        .messages(asLong(q.get("messages")))
        .messagesReady(asLong(q.get("messages_ready")))
        .messagesUnacknowledged(asLong(q.get("messages_unacknowledged")))
        .consumers(asInt(q.get("consumers")))
        .state(asString(q.get("state")))
        .arguments(asMap(q.get("arguments")))
        .policy(asString(q.get("policy")))
        .durable(asBool(q.get("durable")))
        .autoDelete(asBool(q.get("auto_delete")))
        .exclusive(asBool(q.get("exclusive")))
        .build();
  }

  private void validateName(String value, String label) {
    if (value == null || value.isBlank()) {
      throw new ValidationException(label + " bos olamaz");
    }
    // Minimum guvenlik: path traversal / control char engelle
    if (value.contains("/") || value.contains("..") || value.contains("\n") || value.contains("\r")) {
      throw new ValidationException(label + " gecersiz karakter iceriyor");
    }
  }

  private static String asString(Object o) {
    return o == null ? null : o.toString();
  }

  private static Long asLong(Object o) {
    if (o == null) return null;
    if (o instanceof Number n) return n.longValue();
    try { return Long.parseLong(o.toString()); } catch (NumberFormatException e) { return null; }
  }

  private static Integer asInt(Object o) {
    if (o == null) return null;
    if (o instanceof Number n) return n.intValue();
    try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return null; }
  }

  private static Boolean asBool(Object o) {
    if (o == null) return null;
    if (o instanceof Boolean b) return b;
    return Boolean.valueOf(o.toString());
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> asMap(Object o) {
    if (o instanceof Map<?, ?> m) {
      return (Map<String, Object>) m;
    }
    return Map.of();
  }

  /**
   * RabbitMQ management API erisim hatalarini 503 ile disa aksettirir.
   * GlobalExceptionHandler `BaseException` ailesini zaten isler.
   */
  public static class BrokerUnavailableException extends BaseException {
    public BrokerUnavailableException(String message) {
      super(message, org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "BROKER_UNAVAILABLE");
    }
    public BrokerUnavailableException(String message, Throwable cause) {
      super(message, cause, org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "BROKER_UNAVAILABLE");
    }
  }
}
