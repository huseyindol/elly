package com.cms.service;

import java.util.List;

import com.cms.dto.DtoMailAccountRequest;
import com.cms.dto.DtoMailAccountResponse;
import com.cms.entity.MailAccount;

public interface IMailAccountService {

  DtoMailAccountResponse create(DtoMailAccountRequest request);

  DtoMailAccountResponse update(Long id, DtoMailAccountRequest request);

  DtoMailAccountResponse getById(Long id);

  /** Ham entity dondurur — mail gonderme altyapisinin ic kullanimi icindir. */
  MailAccount getEntityById(Long id);

  List<DtoMailAccountResponse> getAll();

  /** Form yaratirken secime sunulacak aktif hesaplar. */
  List<DtoMailAccountResponse> getAllActive();

  Boolean delete(Long id);

  /**
   * SMTP baglantisini dogrular — mail gondermeden sunucuya baglanir.
   * Baglanti basarisizsa {@link com.cms.exception.BadRequestException} firlatir.
   */
  boolean testConnection(Long id);
}
