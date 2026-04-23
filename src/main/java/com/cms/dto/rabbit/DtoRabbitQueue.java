package com.cms.dto.rabbit;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RabbitMQ queue ozeti. Panel UI queue listesi ve detay drawer'i icin.
 * Alanlar RabbitMQ Management HTTP API (/api/queues) response'undan map edilir.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoRabbitQueue {

  private String name;
  private String vhost;

  /** Toplam mesaj (ready + unacknowledged). */
  private Long messages;

  private Long messagesReady;
  private Long messagesUnacknowledged;

  private Integer consumers;

  /** "running" | "idle" | "flow" | ... */
  private String state;

  /** x-dead-letter-exchange, x-message-ttl, vs. */
  private Map<String, Object> arguments;

  private String policy;

  private Boolean durable;
  private Boolean autoDelete;
  private Boolean exclusive;
}
