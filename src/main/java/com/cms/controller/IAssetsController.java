package com.cms.controller;

import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DtoAssets;
import com.cms.dto.DtoAssetsIU;
import com.cms.entity.RootEntityResponse;

public interface IAssetsController {
  public RootEntityResponse<DtoAssets> createAssets(DtoAssetsIU dtoAssetsIU, MultipartFile file);

  public RootEntityResponse<DtoAssets> updateAssets(Long id, MultipartFile file);

  public RootEntityResponse<Boolean> deleteAssets(Long id);

  public RootEntityResponse<DtoAssets> getAssetsById(Long id);

  public RootEntityResponse<DtoAssets> getAssetsByName(String name);
}
