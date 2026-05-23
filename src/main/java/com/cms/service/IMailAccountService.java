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

  /** Belirli bir tenant'a ait tüm hesaplar. */
  List<DtoMailAccountResponse> getAllByTenantId(String tenantId);

  /** Form yaratirken secime sunulacak aktif hesaplar. */
  List<DtoMailAccountResponse> getAllActive();

  /** Belirli bir tenant'a ait aktif hesaplar — doğrulama e-postası vb. için. */
  List<DtoMailAccountResponse> getAllActiveByTenantId(String tenantId);

  Boolean delete(Long id);

  /**
   * SMTP baglantisini dogrular — mail gondermeden sunucuya baglanir.
   * Baglanti basarisizsa {@link com.cms.exception.BadRequestException} firlatir.
   */
  boolean testConnection(Long id);
}
