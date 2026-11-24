package com.cms.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.cms.exception.TooManyRequestsException;

/**
 * Form gönderim limiti (dakikada 5) ziyaretçiye göre olmalı. Prod'da istekler bir iç proxy adresinden
 * geldiği için X-Forwarded-For / remoteAddr her ziyaretçi için aynıydı: limit, o formu dolduran herkesin
 * paylaştığı tek sayaç oluyordu — 5 gönderimden sonra form dürüst ziyaretçilere 429 dönerdi.
 */
class FormSpamProtectionClientIpTest {

  @AfterEach
  void clearRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  private static MockHttpServletRequest productionLikeRequest(String visitorIp) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("10.42.0.1");                        // k3s ServiceLB'nin gördüğü iç adres
    request.addHeader("X-Forwarded-For", "10.42.0.1");        // Traefik'in yazdığı değer
    request.addHeader("CF-Connecting-IP", visitorIp);         // Cloudflare'in yazdığı gerçek ziyaretçi
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    return request;
  }

  private static void submit(FormSpamProtectionService service, long formId) {
    service.verifyAndSanitizeSubmission(formId, new HashMap<String, Object>());
  }

  @Test
  void limitCountsPerRealVisitorNotPerTenant() {
    FormSpamProtectionService service = new FormSpamProtectionService(null); // bellek içi sayaç
    long formId = 9001L;

    // Ziyaretçi A dakikadaki 5 gönderimini yapar.
    for (int i = 0; i < 5; i++) {
      productionLikeRequest("203.0.113.10");
      assertDoesNotThrow(() -> submit(service, formId), "A #" + (i + 1));
    }

    // A'nın 6. denemesi sınırda — ama ziyaretçi B'yi engellememeli.
    assertThrows(TooManyRequestsException.class, () -> submit(service, formId), "A #6");

    for (int i = 0; i < 5; i++) {
      productionLikeRequest("203.0.113.20");
      assertDoesNotThrow(() -> submit(service, formId), "B #" + (i + 1));
    }
    assertThrows(TooManyRequestsException.class, () -> submit(service, formId), "B #6");

    // IPv6 ziyaretçi de kendi sayacını kullanır.
    productionLikeRequest("2a00:1d34:3563:5000::1");
    assertDoesNotThrow(() -> submit(service, formId));
  }

  @Test
  void withoutVisitorHeaderEveryoneSharesOneBucket() {
    // Geri uyumluluk / yerel geliştirme: Cloudflare başlığı yoksa davranış eskisi gibi (bağlantı adresine göre).
    FormSpamProtectionService service = new FormSpamProtectionService(null);
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("192.0.2.7");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    for (int i = 0; i < 5; i++) {
      assertDoesNotThrow(() -> submit(service, 9002L));
    }
    assertThrows(TooManyRequestsException.class, () -> submit(service, 9002L));
  }
}
