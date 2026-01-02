package com.cms.mapper;

import java.util.List;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import com.cms.dto.DtoPost;
import com.cms.dto.DtoPostIU;
import com.cms.entity.Post;

@Mapper(componentModel = "spring")
public interface PostMapper {
  DtoPost toDtoPost(Post post);

  @Named("toDtoPostSimple")
  @Mapping(target = "seoInfo", ignore = true)
  DtoPost toDtoPostSimple(Post post);

  Post toPost(DtoPostIU dtoPostIU);

  void updatePostFromDto(DtoPostIU dtoPostIU, @MappingTarget Post post);

  List<DtoPost> toDtoPostList(List<Post> posts);

  @IterableMapping(qualifiedByName = "toDtoPostSimple")
  List<DtoPost> toDtoPostListSimple(List<Post> posts);
}
