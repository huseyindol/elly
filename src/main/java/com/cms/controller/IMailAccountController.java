package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoMailAccountRequest;
import com.cms.dto.DtoMailAccountResponse;
import com.cms.dto.DtoMailTestRequest;
import com.cms.entity.RootEntityResponse;

import jakarta.validation.Valid;

public interface IMailAccountController {

  RootEntityResponse<DtoMailAccountResponse> create(@Valid DtoMailAccountRequest request);

  RootEntityResponse<DtoMailAccountResponse> update(Long id, @Valid DtoMailAccountRequest request);

  RootEntityResponse<DtoMailAccountResponse> getById(Long id);

  RootEntityResponse<List<DtoMailAccountResponse>> getAll();

  /** Form yaratirken secime sunulacak aktif hesaplar. */
  RootEntityResponse<List<DtoMailAccountResponse>> getAllActive();

  RootEntityResponse<Boolean> delete(Long id);

  /** Gercek SMTP baglantisi acarak testTo adresine deneme maili gonderir. */
  RootEntityResponse<String> testConnection(Long id, @Valid DtoMailTestRequest request);

  /** Mail gondermeden SMTP baglantisini dogrular. */
  RootEntityResponse<String> verifyConnection(Long id);
}
