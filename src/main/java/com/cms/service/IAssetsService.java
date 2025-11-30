package com.cms.service;

import org.springframework.web.multipart.MultipartFile;

import com.cms.entity.Assets;

public interface IAssetsService {
  Assets saveAssets(Assets assets, MultipartFile file);

  Boolean deleteAssets(Long id);

  Assets getAssetsById(Long id);

  Assets getAssetsByName(String name);

}
