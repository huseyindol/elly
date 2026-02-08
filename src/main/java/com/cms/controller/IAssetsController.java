package com.cms.controller;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.cms.dto.DtoAssets;
import com.cms.dto.DtoAssetsIU;
import com.cms.dto.PagedResponse;
import com.cms.entity.RootEntityResponse;

public interface IAssetsController {
  RootEntityResponse<DtoAssets> createAssets(DtoAssetsIU dtoAssetsIU, MultipartFile file);

  RootEntityResponse<List<DtoAssets>> createMultipleAssets(DtoAssetsIU dtoAssetsIU, List<MultipartFile> files);

  RootEntityResponse<DtoAssets> updateAssets(Long id, MultipartFile file);

  RootEntityResponse<Boolean> deleteAssets(Long id);

  RootEntityResponse<DtoAssets> getAssetsById(Long id);

  RootEntityResponse<List<DtoAssets>> getAssetsByName(String name);

  RootEntityResponse<PagedResponse<DtoAssets>> getAssetsByNamePaged(String name, int page, int size, String sort);

  RootEntityResponse<List<DtoAssets>> getAssetsBySubFolder(String subFolder);

  RootEntityResponse<PagedResponse<DtoAssets>> getAssetsBySubFolderPaged(String subFolder, int page, int size,
      String sort);

  RootEntityResponse<PagedResponse<DtoAssets>> getAssetsBySubFolderAndNamePaged(String subFolder, String name, int page,
      int size, String sort);

  RootEntityResponse<List<String>> getAllSubFolders();

  RootEntityResponse<List<DtoAssets>> getAllAssets();

  RootEntityResponse<PagedResponse<DtoAssets>> getAllAssetsPaged(int page, int size, String sort);
}
