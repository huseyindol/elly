package com.cms.config;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import com.cms.entity.MailAccount;
import com.cms.util.AesEncryptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link MailAccount} entity'sinden {@link JavaMailSender} üretir ve önbelleğe alır.
 *
 * <p>Cache key: {@code mailAccount.id} — update/delete sonrası
 * {@link #evict(Long)} çağrılarak geçersiz kılınır.
 *
 * <p>Şifre çözme: {@link AesEncryptor} ile AES-256-CBC.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantMailSenderFactory {

  private final AesEncryptor aesEncryptor;

  private final ConcurrentHashMap<Long, JavaMailSender> cache = new ConcurrentHashMap<>();

  /**
   * Verilen {@link MailAccount} için {@link JavaMailSender} döndürür.
   * İlk çağrıda oluşturulur ve önbelleğe alınır.
   */
  public JavaMailSender getMailSender(MailAccount account) {
    return cache.computeIfAbsent(account.getId(), id -> buildSender(account));
  }

  /**
   * Belirtilen hesabın önbelleğini temizler.
   * Hesap güncellendiğinde veya silindiğinde servis katmanından çağrılır.
   */
  public void evict(Long mailAccountId) {
    cache.remove(mailAccountId);
    log.debug("JavaMailSender önbelleği temizlendi: mailAccountId={}", mailAccountId);
  }

  private JavaMailSender buildSender(MailAccount account) {
    String rawPassword = aesEncryptor.decrypt(account.getSmtpPassword());

    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(account.getSmtpHost());
    sender.setPort(account.getSmtpPort());
    sender.setUsername(account.getSmtpUsername());
    sender.setPassword(rawPassword);
    sender.setDefaultEncoding("UTF-8");

    Properties props = sender.getJavaMailProperties();
    props.put("mail.transport.protocol", "smtp");
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.starttls.required", "true");

    log.info("JavaMailSender oluşturuldu: mailAccountId={}, host={}:{}",
        account.getId(), account.getSmtpHost(), account.getSmtpPort());
    return sender;
  }
}
