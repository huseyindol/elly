package com.cms.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.entity.Assets;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.AssetsRepository;
import com.cms.service.IAssetsService;
import com.cms.service.IFileService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssetsService implements IAssetsService {

  private final IFileService fileService;
  private final AssetsRepository assetsRepository;

  @Override
  @Transactional
  public Assets saveAssets(Assets assets, MultipartFile file) {
    String path = fileService.saveFile(file, assets.getSubFolder());
    assets.setPath(path);
    Assets savedAssets = assetsRepository.save(assets);
    return savedAssets;
  }

  @Override
  @Transactional
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
        .orElseThrow(() -> new ResourceNotFoundException("Assets", id));
  }

  @Override
  public Assets getAssetsByName(String name) {
    return assetsRepository.findByName(name)
        .orElseThrow(() -> new ResourceNotFoundException("Assets", "name", name));
  }

}
