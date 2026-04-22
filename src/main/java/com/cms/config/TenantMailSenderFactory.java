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
 * {@link MailAccount} DB kaydindan {@link JavaMailSender} uretir ve cache'ler
 * (Mail+Form v2).
 *
 * <p>Cache key: {@code mailAccountId}. Hesap guncellendiginde veya silindiginde
 * {@link #evict(Long)} cagrilarak cache gecersiz kilinir. Sifre DB'de
 * AES-256-CBC sifreli tutuldugu icin {@link AesEncryptor#decrypt(String)} ile
 * cozulup JavaMail'e verilir — sifre memory'de ancak bu build sirasinda tutulur.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantMailSenderFactory {

  private final AesEncryptor aesEncryptor;

  private final ConcurrentHashMap<Long, JavaMailSender> cache = new ConcurrentHashMap<>();

  /**
   * {@link MailAccount} entity'sinden {@link JavaMailSender} doner.
   * Cache'de varsa onu, yoksa yenisini olusturup cache'ler.
   */
  public JavaMailSender getMailSender(MailAccount account) {
    if (account == null || account.getId() == null) {
      throw new IllegalArgumentException("MailAccount ve id zorunludur");
    }
    Long id = account.getId();
    JavaMailSender cached = cache.get(id);
    if (cached != null) {
      return cached;
    }
    JavaMailSender built = buildSender(account);
    cache.put(id, built);
    return built;
  }

  /**
   * Belirli bir hesap icin cache'i temizler. Hesap guncellenirse / silinirse
   * cagrilmalidir — eski credential'larla bagli sender cache'te kalmasin.
   */
  public void evict(Long mailAccountId) {
    if (mailAccountId == null) {
      return;
    }
    cache.remove(mailAccountId);
    log.debug("JavaMailSender cache evicted: mailAccountId={}", mailAccountId);
  }

  /** Cache'i komple temizler (test veya full refresh icin). */
  public void evictAll() {
    cache.clear();
    log.debug("JavaMailSender cache fully evicted");
  }

  private JavaMailSender buildSender(MailAccount account) {
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(account.getSmtpHost());
    sender.setPort(account.getSmtpPort());
    sender.setUsername(account.getSmtpUsername());
    sender.setPassword(aesEncryptor.decrypt(account.getSmtpPassword()));
    sender.setDefaultEncoding("UTF-8");

    Properties mailProps = sender.getJavaMailProperties();
    mailProps.put("mail.transport.protocol", "smtp");
    mailProps.put("mail.smtp.auth", "true");

    int port = account.getSmtpPort() == null ? 587 : account.getSmtpPort();
    if (port == 465) {
      // SSL/TLS
      mailProps.put("mail.smtp.ssl.enable", "true");
      mailProps.put("mail.smtp.ssl.trust", account.getSmtpHost());
    } else {
      // STARTTLS (Gmail 587)
      mailProps.put("mail.smtp.starttls.enable", "true");
      mailProps.put("mail.smtp.starttls.required", "true");
    }

    mailProps.put("mail.smtp.connectiontimeout", "10000");
    mailProps.put("mail.smtp.timeout", "10000");
    mailProps.put("mail.smtp.writetimeout", "10000");

    // Sadece host + port + id log'lanir — username/password asla log'a dusmez.
    log.info("JavaMailSender olusturuldu: mailAccountId={}, host={}:{}, ssl={}",
        account.getId(), account.getSmtpHost(), port, port == 465);
    return sender;
  }
}
