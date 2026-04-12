package com.cms.service.impl;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.TenantMailSenderFactory;
import com.cms.dto.DtoMailAccountRequest;
import com.cms.dto.DtoMailAccountResponse;
import com.cms.entity.MailAccount;
import com.cms.exception.BadRequestException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.mapper.MailAccountMapper;
import com.cms.repository.MailAccountRepository;
import com.cms.service.IMailAccountService;
import com.cms.util.AesEncryptor;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailAccountService implements IMailAccountService {

  private final MailAccountRepository repository;
  private final MailAccountMapper mapper;
  private final AesEncryptor aesEncryptor;
  private final TenantMailSenderFactory mailSenderFactory;

  @Override
  @Transactional
  @CacheEvict(value = "mailAccounts", allEntries = true)
  public DtoMailAccountResponse create(DtoMailAccountRequest request) {
    if (request.getSmtpPassword() == null || request.getSmtpPassword().isBlank()) {
      throw new IllegalArgumentException("Oluşturma sırasında SMTP şifresi zorunludur");
    }

    if (Boolean.TRUE.equals(request.getIsDefault())) {
      repository.clearAllDefaults();
    }

    MailAccount entity = mapper.toEntity(request);
    entity.setSmtpPassword(aesEncryptor.encrypt(request.getSmtpPassword()));

    MailAccount saved = repository.save(entity);
    log.info("Mail hesabı oluşturuldu: id={}, name={}", saved.getId(), saved.getName());
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  @CacheEvict(value = "mailAccounts", allEntries = true)
  public DtoMailAccountResponse update(Long id, DtoMailAccountRequest request) {
    MailAccount entity = findOrThrow(id);

    if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(entity.getIsDefault())) {
      repository.clearAllDefaults();
    }

    mapper.updateFromRequest(request, entity);

    // Şifre alanı dolu gelirse güncelle, boşsa eskiyi koru
    if (request.getSmtpPassword() != null && !request.getSmtpPassword().isBlank()) {
      entity.setSmtpPassword(aesEncryptor.encrypt(request.getSmtpPassword()));
    }

    MailAccount saved = repository.save(entity);
    mailSenderFactory.evict(saved.getId());
    log.info("Mail hesabı güncellendi: id={}, name={}", saved.getId(), saved.getName());
    return mapper.toResponse(saved);
  }

  @Override
  @Cacheable(value = "mailAccounts", key = "#id")
  public DtoMailAccountResponse getById(Long id) {
    return mapper.toResponse(findOrThrow(id));
  }

  @Override
  public MailAccount getEntityById(Long id) {
    return findOrThrow(id);
  }

  @Override
  @Cacheable(value = "mailAccounts", key = "'all'")
  public List<DtoMailAccountResponse> getAll() {
    return mapper.toResponseList(repository.findAll());
  }

  @Override
  @Transactional
  @CacheEvict(value = "mailAccounts", allEntries = true)
  public Boolean delete(Long id) {
    MailAccount entity = findOrThrow(id);
    repository.delete(entity);
    mailSenderFactory.evict(id);
    log.info("Mail hesabı silindi: id={}", id);
    return true;
  }

  @Override
  @Transactional
  @CacheEvict(value = "mailAccounts", allEntries = true)
  public DtoMailAccountResponse setDefault(Long id) {
    MailAccount entity = findOrThrow(id);
    if (!Boolean.TRUE.equals(entity.getActive())) {
      throw new IllegalStateException("Pasif bir hesap varsayılan yapılamaz");
    }
    repository.clearAllDefaults();
    entity.setIsDefault(true);
    return mapper.toResponse(repository.save(entity));
  }

  @Override
  public MailAccount getDefaultEntity() {
    return repository.findByIsDefaultTrueAndActiveTrue()
        .orElseThrow(() -> new ResourceNotFoundException(
            "Varsayılan aktif mail hesabı bulunamadı. Lütfen panelden bir hesap ekleyin ve varsayılan olarak işaretleyin."));
  }

  @Override
  public boolean testConnection(Long id) {
    MailAccount account = findOrThrow(id);
    JavaMailSenderImpl sender = (JavaMailSenderImpl) mailSenderFactory.getMailSender(account);
    try {
      sender.testConnection();
      log.info("SMTP bağlantısı başarılı: hesap='{}', host={}:{}",
          account.getName(), account.getSmtpHost(), account.getSmtpPort());
      return true;
    } catch (MessagingException e) {
      log.error("SMTP bağlantısı başarısız: hesap='{}', hata='{}'",
          account.getName(), e.getMessage());
      throw new BadRequestException(
          "SMTP bağlantısı başarısız [" + account.getSmtpHost() + ":" + account.getSmtpPort() + "]: "
              + e.getMessage());
    }
  }

  private MailAccount findOrThrow(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("MailAccount", id));
  }
}
