package com.cms.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

/**
 * RFC 6238 (TOTP) ve RFC 4648 (Base32) implementasyonu.
 * Harici kütüphane gerekmez — Java standart kütüphanesi yeterli.
 * Google Authenticator / Authy / Microsoft Authenticator ile uyumlu.
 */
@Service
public class TotpService {

  private static final String ISSUER = "Elly CMS";
  private static final int CODE_DIGITS = 6;
  private static final int TIME_STEP_SEC = 30;
  /** ±1 adım (30sn) saat kaymasına tolerans */
  private static final int WINDOW = 1;
  private static final String BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

  // ── Secret üretimi ─────────────────────────────────────────────────────

  /**
   * 20 rastgele bayttan Base32 secret üretir (~32 karakter).
   * Sonuç AesEncryptor ile şifreli saklanmalıdır.
   */
  public String generateSecret() {
    byte[] bytes = new byte[20];
    new SecureRandom().nextBytes(bytes);
    return base32Encode(bytes);
  }

  // ── QR URI ─────────────────────────────────────────────────────────────

  /**
   * Google Authenticator uyumlu otpauth URI üretir.
   * Frontend bu URI'yi qrcode.js ile QR'a dönüştürür.
   *
   * @param secret Base32 secret (düz metin, şifrelenmemiş)
   * @param username kullanıcı adı (QR üzerinde görünür)
   * @return otpauth://totp/... formatında URI
   */
  public String buildQrUri(String secret, String username) {
    String label = URLEncoder.encode(ISSUER + ":" + username, StandardCharsets.UTF_8);
    String issuerEncoded = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
    return "otpauth://totp/" + label
        + "?secret=" + secret
        + "&issuer=" + issuerEncoded
        + "&algorithm=SHA1"
        + "&digits=" + CODE_DIGITS
        + "&period=" + TIME_STEP_SEC;
  }

  // ── Kod doğrulama ──────────────────────────────────────────────────────

  /**
   * Kullanıcının girdiği 6 haneli kodu doğrular.
   * ±1 zaman adımı (saat kayması) toleransı vardır.
   *
   * @param secret   Base32 secret (düz metin, şifre çözülmüş)
   * @param userCode kullanıcıdan alınan 6 haneli kod
   * @return true ise kod geçerli
   */
  public boolean verify(String secret, String userCode) {
    if (userCode == null || userCode.length() != CODE_DIGITS) return false;
    int inputCode;
    try {
      inputCode = Integer.parseInt(userCode.trim());
    } catch (NumberFormatException e) {
      return false;
    }

    byte[] keyBytes;
    try {
      keyBytes = base32Decode(secret);
    } catch (Exception e) {
      return false;
    }

    long counter = System.currentTimeMillis() / 1000 / TIME_STEP_SEC;
    for (int delta = -WINDOW; delta <= WINDOW; delta++) {
      if (generateCode(keyBytes, counter + delta) == inputCode) {
        return true;
      }
    }
    return false;
  }

  // ── RFC 6238 TOTP / RFC 2104 HMAC ──────────────────────────────────────

  private int generateCode(byte[] key, long counter) {
    try {
      byte[] data = new byte[8];
      long c = counter;
      for (int i = 7; i >= 0; i--) {
        data[i] = (byte) (c & 0xFF);
        c >>= 8;
      }
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      byte[] hash = mac.doFinal(data);

      // Dynamic truncation
      int offset = hash[hash.length - 1] & 0x0F;
      int truncated = ((hash[offset]     & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    |  (hash[offset + 3] & 0xFF);

      int mod = 1;
      for (int i = 0; i < CODE_DIGITS; i++) mod *= 10;
      return truncated % mod;
    } catch (Exception e) {
      throw new IllegalStateException("TOTP code generation failed", e);
    }
  }

  // ── RFC 4648 Base32 ────────────────────────────────────────────────────

  private String base32Encode(byte[] data) {
    StringBuilder sb = new StringBuilder();
    int buffer = 0, bitsLeft = 0;
    for (byte b : data) {
      buffer = (buffer << 8) | (b & 0xFF);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        bitsLeft -= 5;
        sb.append(BASE32_CHARS.charAt((buffer >> bitsLeft) & 0x1F));
      }
    }
    if (bitsLeft > 0) {
      sb.append(BASE32_CHARS.charAt((buffer << (5 - bitsLeft)) & 0x1F));
    }
    return sb.toString();
  }

  private byte[] base32Decode(String input) {
    String clean = input.toUpperCase().replaceAll("[^A-Z2-7]", "");
    byte[] output = new byte[clean.length() * 5 / 8];
    int buffer = 0, bitsLeft = 0, idx = 0;
    for (char c : clean.toCharArray()) {
      int val = BASE32_CHARS.indexOf(c);
      if (val < 0) continue;
      buffer = (buffer << 5) | val;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        bitsLeft -= 8;
        if (idx < output.length) {
          output[idx++] = (byte) (buffer >> bitsLeft);
        }
      }
    }
    return Arrays.copyOf(output, idx);
  }
}
