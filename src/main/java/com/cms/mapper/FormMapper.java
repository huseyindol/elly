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

@Mapper(componentModel = "spring")
public interface FormMapper {

  // FormDefinition mappings
  DtoFormDefinition toDto(FormDefinition entity);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  FormDefinition toEntity(DtoFormDefinitionIU dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateFromDto(DtoFormDefinitionIU dto, @MappingTarget FormDefinition entity);

  List<DtoFormDefinition> toDtoList(List<FormDefinition> entities);

  // FormSubmission mappings
  @Mapping(target = "formDefinitionId", source = "formDefinition.id")
  @Mapping(target = "formTitle", source = "formDefinition.title")
  DtoFormSubmission toSubmissionDto(FormSubmission entity);

  List<DtoFormSubmission> toSubmissionDtoList(List<FormSubmission> entities);
}
