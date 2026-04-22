package com.cms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoMailAccountRequest;
import com.cms.dto.DtoMailAccountResponse;
import com.cms.entity.MailAccount;

@Mapper(componentModel = "spring")
public interface MailAccountMapper {

  /** Entity -> Response. smtpPassword asla response'a dahil edilmez. */
  DtoMailAccountResponse toResponse(MailAccount entity);

  List<DtoMailAccountResponse> toResponseList(List<MailAccount> entities);

  /**
   * Request -> Entity (yeni kayit).
   * {@code smtpPassword} servis katmaninda AES ile sifrelenip set edilir;
   * burada ignore edilir ki duz metin entity'e sizmasin.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "smtpPassword", ignore = true)
  MailAccount toEntity(DtoMailAccountRequest request);

  /**
   * Guncelleme: request alanlarini mevcut entity'ye uygular.
   * {@code smtpPassword} servis katmaninda yonetilir (bos gelirse korunur).
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "smtpPassword", ignore = true)
  void updateFromRequest(DtoMailAccountRequest request, @MappingTarget MailAccount entity);
}
