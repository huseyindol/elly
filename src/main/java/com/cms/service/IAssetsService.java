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

  Assets getAssetsByName(String name);

  List<Assets> getAllAssets();

  Page<Assets> getAllAssetsPaged(Pageable pageable);
}
