package com.cms.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    // Check if asset already exists in this subfolder
    Optional<Assets> existingAssetsOpt = assetsRepository.findByNameAndSubFolder(
        file.getOriginalFilename(), assets.getSubFolder());

    Assets assetToSave;
    if (existingAssetsOpt.isPresent()) {
      assetToSave = existingAssetsOpt.get();
      // Update fields if needed (path will be updated below)
      assetToSave.setType(file.getContentType());
      assetToSave.setExtension(fileService.getFileExtension(file.getOriginalFilename()));
      // Name and SubFolder are already same
    } else {
      assetToSave = assets;
    }

    String path = fileService.saveFile(file, assetToSave.getSubFolder());
    assetToSave.setPath(path);

    return assetsRepository.save(assetToSave);
  }

  @Override
  @Transactional
  public List<Assets> saveMultipleAssets(String subFolder, List<MultipartFile> files) {
    List<Assets> savedAssetsList = new ArrayList<>();
    for (MultipartFile file : files) {
      // Check if asset already exists
      Optional<Assets> existingAssetsOpt = assetsRepository.findByNameAndSubFolder(
          file.getOriginalFilename(), subFolder);

      Assets assetToSave;
      if (existingAssetsOpt.isPresent()) {
        assetToSave = existingAssetsOpt.get();
        assetToSave.setType(file.getContentType());
        assetToSave.setExtension(fileService.getFileExtension(file.getOriginalFilename()));
      } else {
        assetToSave = new Assets();
        assetToSave.setName(file.getOriginalFilename());
        assetToSave.setType(file.getContentType());
        assetToSave.setSubFolder(subFolder);
        assetToSave.setExtension(fileService.getFileExtension(file.getOriginalFilename()));
      }

      String path = fileService.saveFile(file, subFolder);
      assetToSave.setPath(path);

      Assets savedAssets = assetsRepository.save(assetToSave);
      savedAssetsList.add(savedAssets);
    }
    return savedAssetsList;
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
  public List<Assets> getAssetsByName(String name) {
    return assetsRepository.findByNameContainingIgnoreCase(name);
  }

  @Override
  public Page<Assets> getAssetsByNamePaged(String name, Pageable pageable) {
    return assetsRepository.findByNameContainingIgnoreCase(name, pageable);
  }

  @Override
  public List<Assets> getAssetsBySubFolder(String subFolder) {
    return assetsRepository.findBySubFolder(subFolder);
  }

  @Override
  public Page<Assets> getAssetsBySubFolderPaged(String subFolder, Pageable pageable) {
    return assetsRepository.findBySubFolder(subFolder, pageable);
  }

  @Override
  public Page<Assets> getAssetsBySubFolderAndNamePaged(String subFolder, String name, Pageable pageable) {
    return assetsRepository.findBySubFolderAndNameContainingIgnoreCase(subFolder, name, pageable);
  }

  @Override
  public List<String> getAllSubFolders() {
    return assetsRepository.findDistinctSubFolders();
  }

  @Override
  public List<Assets> getAllAssets() {
    List<Assets> assets = assetsRepository.findAll();
    return assets;
  }

  @Override
  public Page<Assets> getAllAssetsPaged(
      Pageable pageable) {
    return assetsRepository.findAll(pageable);
  }

}
