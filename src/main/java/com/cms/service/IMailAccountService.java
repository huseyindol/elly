package com.cms.service;

import java.util.List;

import com.cms.dto.DtoMailAccountRequest;
import com.cms.dto.DtoMailAccountResponse;
import com.cms.entity.MailAccount;

public interface IMailAccountService {

  DtoMailAccountResponse create(DtoMailAccountRequest request);

  DtoMailAccountResponse update(Long id, DtoMailAccountRequest request);

  DtoMailAccountResponse getById(Long id);

  /** Ham entity döndürür — mail gönderme altyapısının iç kullanımı içindir. */
  MailAccount getEntityById(Long id);

  List<DtoMailAccountResponse> getAll();

  Boolean delete(Long id);

  /** Belirtilen hesabı varsayılan yapar, eskinin varsayılan bayrağını temizler. */
  DtoMailAccountResponse setDefault(Long id);

  /**
   * Varsayılan (is_default=true, active=true) hesabı döndürür.
   * Bulunamazsa {@link com.cms.exception.ResourceNotFoundException} fırlatır.
   */
  MailAccount getDefaultEntity();
}
