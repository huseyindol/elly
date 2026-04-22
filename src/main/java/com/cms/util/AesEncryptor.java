package com.cms.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-CBC ile sifreleme / cozme yardimcisi.
 *
 * <p>Saklama formati: {@code base64(IV) + ":" + base64(ciphertext)}
 *
 * <p>Yapilandirma: {@code AES_SECRET_KEY} ortam degiskeni tam olarak
 * 32 ASCII karakter (256 bit) olmalidir.
 *
 * <p>Mail+Form v2: {@code mail_accounts.smtp_password} bu encoder ile sifreli
 * saklanir. Ham deger asla DTO/response'larda disariya cikmaz.
 */
@Component
public class AesEncryptor {

  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
  private static final int IV_LENGTH = 16;

  private final SecretKeySpec secretKey;

  public AesEncryptor(@Value("${aes.secret-key}") String rawKey) {
    byte[] keyBytes = rawKey.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length != 32) {
      throw new IllegalArgumentException(
          "AES_SECRET_KEY tam 32 karakter (256 bit) olmalidir, ancak "
              + keyBytes.length + " byte alindi");
    }
    this.secretKey = new SecretKeySpec(keyBytes, "AES");
  }

  /**
   * Duz metni AES-256-CBC ile sifreler.
   *
   * @param plainText sifrelenecek metin
   * @return {@code base64(IV):base64(ciphertext)} formatinda sifreli metin
   */
  public String encrypt(String plainText) {
    try {
      byte[] iv = new byte[IV_LENGTH];
      new SecureRandom().nextBytes(iv);
      IvParameterSpec ivSpec = new IvParameterSpec(iv);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
      byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

      return Base64.getEncoder().encodeToString(iv)
          + ":"
          + Base64.getEncoder().encodeToString(encrypted);
    } catch (Exception e) {
      throw new IllegalStateException("Sifreleme basarisiz", e);
    }
  }

  /**
   * {@code base64(IV):base64(ciphertext)} formatindaki sifreli metni cozer.
   *
   * @param encryptedText sifreli metin
   * @return duz metin
   */
  public String decrypt(String encryptedText) {
    try {
      String[] parts = encryptedText.split(":", 2);
      if (parts.length != 2) {
        throw new IllegalArgumentException("Gecersiz sifreli metin formati");
      }
      byte[] iv = Base64.getDecoder().decode(parts[0]);
      byte[] cipherText = Base64.getDecoder().decode(parts[1]);

      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
      return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Sifre cozme basarisiz", e);
    }
  }
}
