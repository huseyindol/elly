package com.cms.dto.rabbit;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tek bir RabbitMQ mesaji (peek sonucu).
 * Management API /api/queues/{vhost}/{name}/get response'undan map edilir.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DtoRabbitMessage {

  /** Mesaj icerigi. ackmode=ack_requeue_true ile alindigi icin queue'dan silinmez. */
  private String payload;

  /** "string" | "base64" */
  private String payloadEncoding;

  /** headers, content_type, message_id, timestamp, vs. */
  private Map<String, Object> properties;

  /** Peek anindan sonra queue'da kalan mesaj sayisi. */
  private Long messageCount;

  private String routingKey;

  private Boolean redelivered;

  private String exchange;
}
