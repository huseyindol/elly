package com.cms.enums;

public enum WidgetTypeEnum {
  BANNER,
  POST,
  ARTICLE;

  public static WidgetTypeEnum fromString(String type) {
    return WidgetTypeEnum.valueOf(type.toUpperCase());
  }

  public static String toString(WidgetTypeEnum type) {
    return type.name().toLowerCase();
  }
}
