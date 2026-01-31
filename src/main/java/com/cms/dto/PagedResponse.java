package com.cms.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.domain.Page;

/**
 * Generic pagination response wrapper.
 * Performant: Page nesnesinden doğrudan dönüşüm sağlar.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private boolean first;
  private boolean last;

  /**
   * Spring Data Page nesnesinden PagedResponse oluşturur.
   * Bu factory method sayesinde controller'da tek satırda dönüşüm yapılabilir.
   */
  public static <T> PagedResponse<T> from(Page<T> page) {
    return new PagedResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast());
  }

  /**
   * İçerik dönüşümü gerektiren durumlar için (Entity -> DTO).
   * Mapper ile birlikte kullanılır.
   */
  public static <T, R> PagedResponse<R> from(Page<T> page, List<R> mappedContent) {
    return new PagedResponse<>(
        mappedContent,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast());
  }
}
