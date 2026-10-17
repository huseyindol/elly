package com.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "ai_usage_logs",
    indexes = {
      @Index(name = "idx_ai_usage_logs_created_at", columnList = "created_at"),
      @Index(name = "idx_ai_usage_logs_action", columnList = "action"),
      @Index(name = "idx_ai_usage_logs_status", columnList = "status"),
      @Index(name = "idx_ai_usage_logs_request_id", columnList = "request_id"),
      @Index(name = "idx_ai_usage_logs_username", columnList = "username")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUsageLog extends BaseEntity {

  /** Aynı HTTP isteğine ait alt Gemini çağrılarını gruplayan UUID (ör. 3 adımlı makale). */
  @Column(name = "request_id", nullable = false, length = 36)
  private String requestId;

  /** Çok adımlı pipeline'da adım sırası (tekli çağrıda 1, makalede 1-2-3). */
  @Column(name = "step_order", nullable = false)
  private Integer stepOrder;

  /** Çağrılan REST endpoint (ör. /api/v1/ai/slug, /api/v1/ai/article). */
  @Column(nullable = false, length = 100)
  private String endpoint;

  /** İşlem / alt ajan kodu (slug, seo, description, alt-text, blog-info, article:outline, article:draft, article:sanitize). */
  @Column(nullable = false, length = 60)
  private String action;

  /** Kullanılan Gemini modeli (ör. gemini-3.8-flash). */
  @Column(nullable = false, length = 60)
  private String model;

  /** İsteği tetikleyen panel kullanıcısı. */
  @Column(length = 100)
  private String username;

  /** İsteğin çalıştığı tenant kimliği. */
  @Column(name = "tenant_id", length = 60)
  private String tenantId;

  /** Kullanıcının endpoint'e gönderdiği ham istek (JSON). */
  @Column(name = "user_input", columnDefinition = "TEXT")
  private String userInput;

  /** Gemini'ye gönderilen tam prompt metni. */
  @Column(name = "prompt_text", columnDefinition = "TEXT")
  private String promptText;

  /** Gemini'den dönen ham metin yanıtı. */
  @Column(name = "response_text", columnDefinition = "TEXT")
  private String responseText;

  /** İstemciye döndürülen ayrıştırılmış/temizlenmiş çıktı (JSON veya metin). */
  @Column(name = "parsed_output", columnDefinition = "TEXT")
  private String parsedOutput;

  /** Gönderilen prompt karakter uzunluğu. */
  @Column(name = "prompt_chars", nullable = false)
  private Integer promptChars;

  /** Dönen yanıt karakter uzunluğu. */
  @Column(name = "response_chars", nullable = false)
  private Integer responseChars;

  /** Gemini usage.total_input_tokens. */
  @Column(name = "input_tokens", nullable = false)
  private Integer inputTokens;

  /** Gemini usage.total_output_tokens. */
  @Column(name = "output_tokens", nullable = false)
  private Integer outputTokens;

  /** Gemini usage.total_thought_tokens (reasoning/düşünme token'ları). */
  @Column(name = "thought_tokens", nullable = false)
  private Integer thoughtTokens;

  /** Gemini usage.total_cached_tokens. */
  @Column(name = "cached_tokens", nullable = false)
  private Integer cachedTokens;

  /** Gemini usage.total_tokens. */
  @Column(name = "total_tokens", nullable = false)
  private Integer totalTokens;

  /** Çağrı süresi (milisaniye). */
  @Column(name = "duration_ms", nullable = false)
  private Long durationMs;

  /** SUCCESS veya ERROR. */
  @Column(nullable = false, length = 20)
  private String status;

  /** Gemini HTTP durum kodu (ör. 200, 429, 500). */
  @Column(name = "http_status")
  private Integer httpStatus;

  /** Hata varsa kısa hata kodu (ör. RATE_LIMIT, PARSE_ERROR, HTTP_403). */
  @Column(name = "error_code", length = 60)
  private String errorCode;

  /** Sanitize edilmiş hata mesajı. */
  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;
}
