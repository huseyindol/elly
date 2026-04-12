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

  RootEntityResponse<Boolean> delete(Long id);

  RootEntityResponse<DtoMailAccountResponse> setDefault(Long id);

  /** Gerçek SMTP bağlantısı açarak testTo adresine deneme maili gönderir. */
  RootEntityResponse<String> testConnection(Long id, @Valid DtoMailTestRequest request);

  /** Mail göndermeden SMTP bağlantısını doğrular. */
  RootEntityResponse<String> verifyConnection(Long id);
}
