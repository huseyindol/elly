package com.cms.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

import com.cms.dto.DtoWidget;
import com.cms.dto.DtoWidgetIU;
import com.cms.entity.Widget;

@Mapper(componentModel = "spring")
public interface WidgetMapper {
  DtoWidget toDtoWidget(Widget widget);

  Widget toWidget(DtoWidgetIU dtoWidgetIU);

  void updateWidgetFromDto(DtoWidgetIU dtoWidgetIU, @MappingTarget Widget widget);

}
