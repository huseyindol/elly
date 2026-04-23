package com.cms.dto.rabbit;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * RabbitMQ mesajini baska (veya ayni) kuyruga yeniden publish etmek icin request body.
 * Genellikle DLQ'dan ana kuyruga geri atmak icin kullanilir.
 */
@Getter
@Setter
@NoArgsConstructor
public class RepublishRequest {

  /** Hedef queue adi (hangi exchange'e publish edilecek — default exchange + routingKey=queue). */
  @NotBlank(message = "targetQueue zorunludur")
  private String targetQueue;

  /** Mesaj payload'i (JSON string). */
  @NotBlank(message = "payload zorunludur")
  private String payload;

  /** Content-type header (default: application/json). */
  private String contentType;
}
