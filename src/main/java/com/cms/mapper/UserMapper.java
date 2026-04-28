package com.cms.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.BeanMapping;

import com.cms.dto.DtoUserResponse;
import com.cms.dto.DtoUserUpdate;
import com.cms.entity.Role;
import com.cms.entity.User;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface UserMapper {

  @Mapping(source = "roles", target = "roles", qualifiedByName = "rolesToNames")
  DtoUserResponse toDtoUserResponse(User user);

  @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
  void updateUserFromDto(DtoUserUpdate dto, @MappingTarget User user);

  @Named("rolesToNames")
  default List<String> rolesToNames(Set<Role> roles) {
    if (roles == null) return List.of();
    return roles.stream()
        .map(Role::getName)
        .sorted()
        .collect(Collectors.toList());
  }
}

