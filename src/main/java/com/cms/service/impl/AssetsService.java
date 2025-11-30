package com.cms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cms.entity.Assets;
import com.cms.repository.AssetsRepository;
import com.cms.service.IAssetsService;
import com.cms.service.IFileService;

@Service
public class AssetsService implements IAssetsService {

  @Autowired
  private IFileService fileService;

  @Autowired
  private AssetsRepository assetsRepository;

  @Override
  public Assets saveAssets(Assets assets, MultipartFile file) {
    String path = fileService.saveFile(file, assets.getSubFolder());
    assets.setPath(path);
    Assets savedAssets = assetsRepository.save(assets);
    return savedAssets;
  }

  @Override
  public Boolean deleteAssets(Long id) {
    if (assetsRepository.existsById(id)) {
      Assets assets = getAssetsById(id);
      if (assets.getPath() != null) {
        fileService.deleteFile(assets.getPath());
      }
      assetsRepository.deleteById(id);
      return true;
    }
    return false;
  }

  @Override
  public Assets getAssetsById(Long id) {
    return assetsRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Assets not found"));
  }

  @Override
  public Assets getAssetsByName(String name) {
    return assetsRepository.findByName(name)
        .orElseThrow(() -> new RuntimeException("Assets not found"));
  }

}
