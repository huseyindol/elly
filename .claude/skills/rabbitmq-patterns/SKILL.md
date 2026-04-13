---
name: rabbitmq-patterns
description: Elly'deki RabbitMQ queue, exchange, retry ve DLQ pattern'leri. Yeni queue ekleme, consumer yazma, retry mekanizması veya mesaj akış sorunlarında otomatik aktif et.
version: 1.0.0
---

# Elly RabbitMQ Patterns

## Mimari

```
Producer (Service)
  → RabbitTemplate.convertAndSend(exchange, routingKey, message)
  → DirectExchange → Queue → @RabbitListener Consumer

Hata durumunda:
  Consumer → retry-exchange → retry-queue (TTL=30sn) → ana exchange → ana queue
  MAX_RETRY aşıldığında:
  Consumer → FAILED status (DB) + DLQ'ya düşer
```

## Mevcut Queue Yapısı (email örneği)

| Bean | Adı | Amacı |
|------|-----|-------|
| `emailQueue` | `email-queue` | Ana iş kuyruğu, DLX=email-dead-letter-exchange |
| `emailRetryQueue` | `email-retry-queue` | Gecikmeli retry (TTL=30sn), DLX=email-exchange |
| `emailDeadLetterQueue` | `email-dead-letter-queue` | Max retry aşılınca son durak |
| `emailExchange` | `email-exchange` | Ana DirectExchange |
| `emailRetryExchange` | `email-retry-exchange` | Retry DirectExchange |
| `emailDeadLetterExchange` | `email-dead-letter-exchange` | DLX |

**Tüm tanımlar:** `com.cms.config.RabbitMQConfig`

## Yeni Queue Ekleme Pattern'i

```java
// 1. RabbitMQConfig'e constant ekle
public static final String XXX_QUEUE = "xxx-queue";
public static final String XXX_EXCHANGE = "xxx-exchange";
public static final String XXX_ROUTING_KEY = "xxx-routing-key";
public static final String XXX_DLQ = "xxx-dead-letter-queue";
public static final String XXX_DLX = "xxx-dead-letter-exchange";
public static final String XXX_RETRY_QUEUE = "xxx-retry-queue";
public static final String XXX_RETRY_EXCHANGE = "xxx-retry-exchange";

// 2. Ana queue — DLX ile
@Bean
public Queue xxxQueue() {
    return QueueBuilder.durable(XXX_QUEUE)
        .withArgument("x-dead-letter-exchange", XXX_DLX)
        .withArgument("x-dead-letter-routing-key", XXX_ROUTING_KEY)
        .build();
}

// 3. Retry queue — TTL + DLX=ana exchange (geri dönsün)
@Bean
public Queue xxxRetryQueue() {
    return QueueBuilder.durable(XXX_RETRY_QUEUE)
        .withArgument("x-dead-letter-exchange", XXX_EXCHANGE)
        .withArgument("x-dead-letter-routing-key", XXX_ROUTING_KEY)
        .withArgument("x-message-ttl", 30_000) // 30 saniye
        .build();
}

// 4. DLQ — durable, argümansız
@Bean
public Queue xxxDeadLetterQueue() {
    return new Queue(XXX_DLQ, true);
}

// 5. Exchange'ler (DirectExchange)
// 6. Binding'ler (queue → exchange, routing key ile)
```

## Consumer Pattern'i

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class XxxQueueService {

    private final RabbitTemplate rabbitTemplate;
    private static final int MAX_RETRY_COUNT = 3;

    @RabbitListener(queues = RabbitMQConfig.XXX_QUEUE)
    public void processMessage(XxxMessage message) {
        // Multi-tenant: consumer başında tenant set et
        TenantContext.setTenantId(message.getTenantId());
        try {
            // İş mantığı
            doWork(message);
        } catch (Exception e) {
            handleRetry(message, e);
        } finally {
            // ZORUNLU: TenantContext temizle
            TenantContext.clear();
        }
    }

    private void handleRetry(XxxMessage message, Exception e) {
        message.setRetryCount(message.getRetryCount() + 1);
        if (message.getRetryCount() > MAX_RETRY_COUNT) {
            log.error("Max retry aşıldı: {}", message);
            // DB'de FAILED olarak işaretle
        } else {
            log.info("Retry kuyruğuna gönderiliyor: deneme={}/{}", 
                message.getRetryCount(), MAX_RETRY_COUNT);
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.XXX_RETRY_EXCHANGE,
                RabbitMQConfig.XXX_ROUTING_KEY,
                message);
        }
    }
}
```

## Kritik Kurallar

1. **Mesaj formatı:** Jackson2JsonMessageConverter — tüm mesajlar JSON
2. **TenantContext:** Consumer başında `setTenantId()`, finally'de `clear()` — ZORUNLU
3. **Retry:** Doğrudan ana queue'ya değil, retry-exchange'e gönder (TTL gecikmesi için)
4. **DLQ:** Max retry aşılınca mesaj otomatik DLQ'ya düşer (ana queue'nun DLX argümanı sayesinde)
5. **Idempotency:** Consumer'lar idempotent olmalı — aynı mesaj birden fazla işlenebilir
6. **Log:** Her adımda `log.info`/`log.error` ile izlenebilirlik sağla
