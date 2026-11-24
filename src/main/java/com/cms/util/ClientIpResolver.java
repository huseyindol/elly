package com.cms.util;

import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

/**
 * IP bazlı limitler için istemci adresi.
 *
 * Prod'da trafik Cloudflare → k3s ServiceLB → Traefik → uygulama yolundan gelir. ServiceLB kaynak adresini
 * düğümün iç adresine (10.42.0.1) çevirir ve Traefik güvenmediği kaynaktan gelen X-Forwarded-For'ı atar;
 * dolayısıyla XFF de remoteAddr da her istekte aynıdır — IP bazlı sayaçlar tüm site için tek sayaç olur
 * (ör. 30 hatalı giriş herkesin girişini kilitler). Gerçek istemci, Cloudflare'ın her istekte üzerine
 * yazdığı CF-Connecting-IP başlığında bozulmadan gelir; bu yüzden önce o okunur.
 *
 * Güven varsayımı: CF-Connecting-IP ancak origin'e 80/443'ten doğrudan (Cloudflare'ı atlayarak)
 * ulaşılamıyorsa tam güvenilir. Kullanıcı başına limitler bu varsayımdan bağımsızdır.
 */
public final class ClientIpResolver {

  private static final Pattern IPV4 = Pattern.compile("(\\d{1,3}\\.){3}\\d{1,3}");
  private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F:.]{2,45}");

  private ClientIpResolver() {
  }

  public static String resolve(HttpServletRequest request) {
    String cf = request.getHeader("CF-Connecting-IP");
    if (cf != null && isLiteral(cf.trim())) {
      return cf.trim();
    }
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      String first = xff.split(",")[0].trim();
      if (isLiteral(first)) {
        return first;
      }
    }
    return request.getRemoteAddr();
  }

  /** Değişmez bir IP (adres değil, ana bilgisayar adı) mı? DNS sorgusu yapılmaz: bozuk değerler sayaca anahtar olamaz. */
  static boolean isLiteral(String s) {
    if (s.contains(":")) {
      // IPv6: en az iki grup ayırıcı olmalı ("ab:cd" gibi tek ayırıcılı metinler IPv6 değildir)
      return (s.contains("::") || s.indexOf(':') != s.lastIndexOf(':')) && IPV6.matcher(s).matches();
    }
    return IPV4.matcher(s).matches();
  }
}
