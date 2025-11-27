package com.cms.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoComment;
import com.cms.dto.DtoCommentIU;
import com.cms.entity.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {
  @Mapping(target = "post", ignore = true)
  @Mapping(target = "parentComment", ignore = true)
  Comment toComment(DtoCommentIU dtoCommentIU);

  @Mapping(target = "parentComment", ignore = true)
  @Mapping(target = "post", ignore = true)
  DtoComment toDtoComment(Comment comment);

  @Mapping(target = "parentComment", ignore = true)
  @Mapping(target = "post", ignore = true)
  List<Comment> toComments(List<DtoComment> dtoComments);

  @Mapping(target = "parentComment", ignore = true)
  @Mapping(target = "post", ignore = true)
  List<DtoComment> toDtoComments(List<Comment> comments);

  @Mapping(target = "parentComment", ignore = true)
  @Mapping(target = "post", ignore = true)
  void updateCommentFromDto(DtoCommentIU dtoCommentIU, @MappingTarget Comment comment);
}
