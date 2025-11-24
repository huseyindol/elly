package com.cms.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoPost;
import com.cms.dto.DtoPostIU;
import com.cms.entity.Post;

@Mapper(componentModel = "spring")
public interface PostMapper {
  DtoPost toDtoPost(Post post);

  Post toPost(DtoPostIU dtoPostIU);

  void updatePostFromDto(DtoPostIU dtoPostIU, @MappingTarget Post post);

}
