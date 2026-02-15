package com.cms.enums;

public enum ComponentTypeEnum {
  BANNER,
  WIDGET,
  FORM;

  public static ComponentTypeEnum fromString(String type) {
    return ComponentTypeEnum.valueOf(type.toUpperCase());
  }

  public static String toString(ComponentTypeEnum type) {
    return type.name().toLowerCase();
  }
}
