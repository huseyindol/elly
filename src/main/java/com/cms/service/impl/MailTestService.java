package com.cms.service.impl;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cms.config.TenantMailSenderFactory;
import com.cms.entity.MailAccount;
import com.cms.exception.BadRequestException;
import com.cms.exception.ValidationException;
import com.cms.service.IMailTestService;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailTestService implements IMailTestService {

  private final TenantMailSenderFactory mailSenderFactory;

  @Override
  public void sendTestEmail(MailAccount account, String testTo) {
    if (!Boolean.TRUE.equals(account.getActive())) {
      throw new ValidationException("Pasif mail hesabi test edilemez");
    }

    try {
      JavaMailSender sender = mailSenderFactory.getMailSender(account);

      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
      helper.setFrom(account.getFromAddress());
      helper.setTo(testTo);
      helper.setSubject("Elly CMS — SMTP Baglanti Testi");
      helper.setText(buildHtml(account), true);

      sender.send(message);
      log.info("Test maili gonderildi: hesap='{}', id={}, alici='{}'",
          account.getName(), account.getId(), testTo);

    } catch (Exception e) {
      log.error("Test maili gonderilemedi: hesap='{}', id={}, hata='{}'",
          account.getName(), account.getId(), e.getMessage());
      throw new BadRequestException(
          "SMTP baglantisi basarisiz [hesap=" + account.getName() + "]: " + e.getMessage());
    }
  }

  private String buildHtml(MailAccount account) {
    return """
        <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto">
          <h2 style="color:#2563eb">SMTP Baglanti Testi Basarili</h2>
          <p>Bu mail <strong>Elly CMS</strong> panel uzerinden gonderilmistir.</p>
          <hr/>
          <table style="font-size:14px;color:#374151">
            <tr><td><b>Hesap adi</b></td><td style="padding-left:16px">%s</td></tr>
            <tr><td><b>Gonderici</b></td><td style="padding-left:16px">%s</td></tr>
            <tr><td><b>SMTP</b></td><td style="padding-left:16px">%s:%d</td></tr>
          </table>
        </div>
        """.formatted(
            account.getName(),
            account.getFromAddress(),
            account.getSmtpHost(),
            account.getSmtpPort());
  }
}
