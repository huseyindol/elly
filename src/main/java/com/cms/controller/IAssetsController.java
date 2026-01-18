package com.cms.controller;

import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DtoAssets;
import com.cms.dto.DtoAssetsIU;
import com.cms.entity.RootEntityResponse;

public interface IAssetsController {
  RootEntityResponse<DtoAssets> createAssets(DtoAssetsIU dtoAssetsIU, MultipartFile file);

  RootEntityResponse<DtoAssets> updateAssets(Long id, MultipartFile file);

  RootEntityResponse<Boolean> deleteAssets(Long id);

  RootEntityResponse<DtoAssets> getAssetsById(Long id);

  RootEntityResponse<DtoAssets> getAssetsByName(String name);
}
