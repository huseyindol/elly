package com.cms.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.cms.entity.Assets;

public interface IAssetsService {
  Assets saveAssets(Assets assets, MultipartFile file);

  List<Assets> saveMultipleAssets(String subFolder, List<MultipartFile> files);

  Boolean deleteAssets(Long id);

  Assets getAssetsById(Long id);

  List<Assets> getAssetsByName(String name);

  Page<Assets> getAssetsByNamePaged(String name, Pageable pageable);

  List<Assets> getAssetsBySubFolder(String subFolder);

  Page<Assets> getAssetsBySubFolderPaged(String subFolder, Pageable pageable);

  Page<Assets> getAssetsBySubFolderAndNamePaged(String subFolder, String name, Pageable pageable);

  List<String> getAllSubFolders();

  List<Assets> getAllAssets();

  Page<Assets> getAllAssetsPaged(Pageable pageable);
}
