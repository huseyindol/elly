package com.cms.dto;

import com.cms.entity.BannerImage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Banner Ekleme/Güncelleme DTO")
public class DtoBannerIU {
  @Schema(description = "Banner başlığı", example = "Banner Başlığı")
  private String title;

  @Schema(description = "Alt metin", example = "Alt metin")
  private String altText;

  @Schema(description = "Banner linki", example = "https://example.com")
  private String link;

  @Schema(description = "Link hedefi", example = "_blank")
  private String target;

  @Schema(description = "Banner tipi", example = "promo")
  private String type;

  @Schema(description = "Sıra numarası", example = "1")
  private int orderIndex;

  @Schema(description = "Aktif mi", example = "true")
  private boolean status;

  private BannerImage images;
}
