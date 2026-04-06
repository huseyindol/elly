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

  /** Entity → Response DTO (smtpPassword dahil edilmez). */
  @Mapping(target = "isDefault", source = "isDefault")
  DtoMailAccountResponse toResponse(MailAccount entity);

  List<DtoMailAccountResponse> toResponseList(List<MailAccount> entities);

  /**
   * Request DTO → Entity (yeni kayıt).
   * smtpPassword servis katmanında şifrelenerek set edilir,
   * burada kasıtlı olarak atlanır.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "smtpPassword", ignore = true)
  MailAccount toEntity(DtoMailAccountRequest request);

  /**
   * Güncelleme: mevcut entity'ye request alanlarını uygular.
   * smtpPassword ve id servis katmanında ayrıca yönetilir.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "smtpPassword", ignore = true)
  void updateFromRequest(DtoMailAccountRequest request, @MappingTarget MailAccount entity);
}
