package com.cms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoFormDefinition;
import com.cms.dto.DtoFormDefinitionIU;
import com.cms.dto.DtoFormSubmission;
import com.cms.entity.FormDefinition;
import com.cms.entity.FormSubmission;

/**
 * Form (definition + submission) DTO <-> Entity donusumleri.
 *
 * <p>Mail+Form v2: {@code senderMailAccount} lookup'i Controller katmaninda
 * {@link com.cms.service.IMailAccountService#getEntityById(Long)} ile yapilir.
 * Response DTO'lari bagli MailAccount'un id/name/fromAddress'ini salt okunur
 * duz alanlar olarak acar.
 */
@Mapper(componentModel = "spring")
public interface FormMapper {

  // ==================== FormDefinition ====================

  @Mapping(target = "senderMailAccountId", source = "senderMailAccount.id")
  @Mapping(target = "senderMailAccountName", source = "senderMailAccount.name")
  @Mapping(target = "senderFromAddress", source = "senderMailAccount.fromAddress")
  DtoFormDefinition toDto(FormDefinition entity);

  /**
   * Request -> Entity: {@code senderMailAccount} Service/Controller katmaninda
   * MailAccount lookup'i ile set edilir; burada ignore.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "senderMailAccount", ignore = true)
  FormDefinition toEntity(DtoFormDefinitionIU dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  @Mapping(target = "senderMailAccount", ignore = true)
  void updateFromDto(DtoFormDefinitionIU dto, @MappingTarget FormDefinition entity);

  List<DtoFormDefinition> toDtoList(List<FormDefinition> entities);

  // ==================== FormSubmission ====================

  @Mapping(target = "formDefinitionId", source = "formDefinition.id")
  @Mapping(target = "formTitle", source = "formDefinition.title")
  DtoFormSubmission toSubmissionDto(FormSubmission entity);

  List<DtoFormSubmission> toSubmissionDtoList(List<FormSubmission> entities);
}
