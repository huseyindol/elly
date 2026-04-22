package com.cms.service.impl;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.TenantMailSenderFactory;
import com.cms.dto.DtoMailAccountRequest;
import com.cms.dto.DtoMailAccountResponse;
import com.cms.entity.MailAccount;
import com.cms.exception.BadRequestException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.exception.ValidationException;
import com.cms.mapper.MailAccountMapper;
import com.cms.repository.MailAccountRepository;
import com.cms.service.IMailAccountService;
import com.cms.util.AesEncryptor;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mail+Form v2 DB-based mail hesabi yonetimi.
 *
 * <p>Tum SMTP alanlari DB'de tutulur; {@code smtpPassword} AES-256-CBC ile
 * sifrelenir ({@link AesEncryptor}). Guncelleme sirasinda {@code smtpPassword}
 * bos/null gelirse mevcut sifre korunur — yani admin her update'ta sifreyi
 * tekrar girmek zorunda kalmaz.
 *
 * <p>Hesap guncellendiginde veya silindiginde {@link TenantMailSenderFactory}
 * cache'i ilgili hesap icin evict edilir.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailAccountService implements IMailAccountService {

  private final MailAccountRepository repository;
  private final MailAccountMapper mapper;
  private final TenantMailSenderFactory mailSenderFactory;
  private final AesEncryptor aesEncryptor;

  @Override
  @Transactional
  @CacheEvict(value = "mailAccounts", allEntries = true)
  public DtoMailAccountResponse create(DtoMailAccountRequest request) {
    if (request.getSmtpPassword() == null || request.getSmtpPassword().isBlank()) {
      throw new ValidationException("smtpPassword zorunludur");
    }

    MailAccount entity = mapper.toEntity(request);
    entity.setSmtpPassword(aesEncryptor.encrypt(request.getSmtpPassword()));
    if (entity.getActive() == null) {
      entity.setActive(true);
    }

    MailAccount saved = repository.save(entity);
    log.info("Mail hesabi olusturuldu: id={}, name={}, host={}:{}",
        saved.getId(), saved.getName(), saved.getSmtpHost(), saved.getSmtpPort());
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  @CacheEvict(value = "mailAccounts", allEntries = true)
  public DtoMailAccountResponse update(Long id, DtoMailAccountRequest request) {
    MailAccount entity = findOrThrow(id);
    String oldPassword = entity.getSmtpPassword();

    mapper.updateFromRequest(request, entity);

    // Sifre bos/null gelirse mevcut sifreyi koru, aksi halde tekrar sifrele.
    if (request.getSmtpPassword() != null && !request.getSmtpPassword().isBlank()) {
      entity.setSmtpPassword(aesEncryptor.encrypt(request.getSmtpPassword()));
    } else {
      entity.setSmtpPassword(oldPassword);
    }

    MailAccount saved = repository.save(entity);
    mailSenderFactory.evict(saved.getId());
    log.info("Mail hesabi guncellendi: id={}, host={}:{}",
        saved.getId(), saved.getSmtpHost(), saved.getSmtpPort());
    return mapper.toResponse(saved);
  }

  @Override
  public DtoMailAccountResponse getById(Long id) {
    return mapper.toResponse(findOrThrow(id));
  }

  @Override
  public MailAccount getEntityById(Long id) {
    return findOrThrow(id);
  }

  @Override
  public List<DtoMailAccountResponse> getAll() {
    return mapper.toResponseList(repository.findAll());
  }

  @Override
  public List<DtoMailAccountResponse> getAllActive() {
    return mapper.toResponseList(repository.findAllByActiveTrue());
  }

  @Override
  @Transactional
  @CacheEvict(value = "mailAccounts", allEntries = true)
  public Boolean delete(Long id) {
    MailAccount entity = findOrThrow(id);
    repository.delete(entity);
    mailSenderFactory.evict(id);
    log.info("Mail hesabi silindi: id={}, name={}", id, entity.getName());
    return true;
  }

  @Override
  public boolean testConnection(Long id) {
    MailAccount account = findOrThrow(id);

    if (!Boolean.TRUE.equals(account.getActive())) {
      throw new ValidationException("Pasif mail hesabi uzerinde SMTP testi yapilamaz");
    }

    JavaMailSender sender = mailSenderFactory.getMailSender(account);
    try {
      ((JavaMailSenderImpl) sender).testConnection();
      log.info("SMTP baglantisi basarili: hesap='{}', host='{}:{}'",
          account.getName(), account.getSmtpHost(), account.getSmtpPort());
      return true;
    } catch (MessagingException e) {
      log.error("SMTP baglantisi basarisiz: hesap='{}', hata='{}'",
          account.getName(), e.getMessage());
      throw new BadRequestException(
          "SMTP baglantisi basarisiz: " + e.getMessage());
    }
  }

  private MailAccount findOrThrow(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("MailAccount", id));
  }
}
