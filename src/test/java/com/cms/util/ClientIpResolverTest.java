package com.cms.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Prod'da XFF ve remoteAddr her istekte aynı iç adrestir (10.42.0.1); gerçek istemci Cloudflare'ın
 * CF-Connecting-IP başlığındadır. IP bazlı sayaçlar bu yüzden tüm site için tek sayaç oluyordu.
 */
class ClientIpResolverTest {

  private static final String NODE_INTERNAL = "10.42.0.1";

  private MockHttpServletRequest request(String remoteAddr) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr(remoteAddr);
    return request;
  }

  @Test
  void cloudflareHeaderWinsOverTheLostProxyChain() {
    MockHttpServletRequest request = request(NODE_INTERNAL);
    request.addHeader("X-Forwarded-For", NODE_INTERNAL); // Traefik'in yazdığı değer
    request.addHeader("CF-Connecting-IP", "88.230.95.83");

    assertEquals("88.230.95.83", ClientIpResolver.resolve(request));
  }

  @Test
  void ipv6ClientIsKeptAsIs() {
    MockHttpServletRequest request = request(NODE_INTERNAL);
    request.addHeader("CF-Connecting-IP", "2a00:1d34:3563:5000::1");

    assertEquals("2a00:1d34:3563:5000::1", ClientIpResolver.resolve(request));
  }

  @Test
  void malformedCloudflareHeaderFallsBackToForwardedForThenRemoteAddr() {
    MockHttpServletRequest withXff = request(NODE_INTERNAL);
    withXff.addHeader("CF-Connecting-IP", "1.2.3.4, 5.6.7.8"); // liste değil tek adres olmalı
    withXff.addHeader("X-Forwarded-For", "203.0.113.9, 198.51.100.4");
    assertEquals("203.0.113.9", ClientIpResolver.resolve(withXff));

    MockHttpServletRequest bare = request(NODE_INTERNAL);
    bare.addHeader("CF-Connecting-IP", "not-an-ip");
    assertEquals(NODE_INTERNAL, ClientIpResolver.resolve(bare));
  }

  @Test
  void nonAddressValuesNeverBecomeCounterKeys() {
    MockHttpServletRequest xff = request("203.0.113.7");
    xff.addHeader("X-Forwarded-For", "evil.example:9000, 10.0.0.1");
    assertEquals("203.0.113.7", ClientIpResolver.resolve(xff));

    MockHttpServletRequest cf = request("203.0.113.7");
    cf.addHeader("CF-Connecting-IP", "  ");
    assertEquals("203.0.113.7", ClientIpResolver.resolve(cf));
  }

  @Test
  void directRequestWithoutProxyHeadersUsesRemoteAddr() {
    assertEquals("198.51.100.20", ClientIpResolver.resolve(request("198.51.100.20")));
  }
}
