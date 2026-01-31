package com.cms.controller;

import java.util.List;

import com.cms.dto.DtoComponent;
import com.cms.dto.DtoComponentIU;
import com.cms.dto.DtoComponentSummary;
import com.cms.dto.PagedResponse;
import com.cms.entity.RootEntityResponse;

public interface IComponentController {
  RootEntityResponse<DtoComponent> createComponent(DtoComponentIU dtoComponentIU);

  RootEntityResponse<DtoComponent> updateComponent(Long id, DtoComponentIU dtoComponentIU);

  RootEntityResponse<Boolean> deleteComponent(Long id);

  RootEntityResponse<DtoComponent> getComponentById(Long id);

  RootEntityResponse<List<DtoComponent>> getAllComponents();

  RootEntityResponse<List<DtoComponentSummary>> getAllComponentsSummary();

  // Paginated endpoints
  RootEntityResponse<PagedResponse<DtoComponent>> getAllComponentsPaged(int page, int size, String sort);

  RootEntityResponse<PagedResponse<DtoComponentSummary>> getAllComponentsSummaryPaged(int page, int size, String sort);
}
