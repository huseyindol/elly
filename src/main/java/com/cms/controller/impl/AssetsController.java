package com.cms.controller.impl;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.controller.IAssetsController;
import com.cms.dto.DtoAssets;
import com.cms.dto.DtoAssetsIU;
import com.cms.entity.Assets;
import com.cms.entity.RootEntityResponse;
import com.cms.mapper.AssetsMapper;
import com.cms.service.IAssetsService;
import com.cms.service.IFileService;

import io.swagger.v3.oas.annotations.parameters.RequestBody;

@RestController
@RequestMapping("/api/v1/assets")
public class AssetsController extends BaseController implements IAssetsController {

  @Autowired
  private IAssetsService assetsService;

  @Autowired
  private AssetsMapper assetsMapper;

  @Autowired
  private IFileService fileService;

  @Override
  @PostMapping(consumes = "multipart/form-data")
  public RootEntityResponse<DtoAssets> createAssets(
      @ParameterObject @ModelAttribute DtoAssetsIU dtoAssetsIU,
      @RequestParam(value = "file", required = true) MultipartFile file) {
    Assets assets = new Assets();
    assets.setName(file.getOriginalFilename());
    assets.setType(file.getContentType());
    assets.setSubFolder(dtoAssetsIU.getSubFolder());
    assets.setExtension(fileService.getFileExtension(file.getOriginalFilename()));
    Assets savedAssets = assetsService.saveAssets(assets, file);
    DtoAssets dtoAssets = assetsMapper.toDtoAssets(savedAssets);
    return ok(dtoAssets);
  }

  @Override
  @PutMapping(value = "/{id}", consumes = "multipart/form-data")
  public RootEntityResponse<DtoAssets> updateAssets(@PathVariable Long id,
      @RequestParam(value = "file", required = true) MultipartFile file) {
    Assets assets = assetsService.getAssetsById(id);
    assets.setName(file.getOriginalFilename());
    assets.setType(file.getContentType());
    fileService.deleteFile(assets.getPath());
    Assets savedAssets = assetsService.saveAssets(assets, file);
    DtoAssets dtoAssets = assetsMapper.toDtoAssets(savedAssets);
    return ok(dtoAssets);
  }

  @Override
  @DeleteMapping("/{id}")
  public RootEntityResponse<Boolean> deleteAssets(@PathVariable Long id) {
    Boolean deleted = assetsService.deleteAssets(id);
    if (deleted) {
      return ok(deleted);
    }
    return error("Assets not deleted");
  }

  @Override
  @GetMapping("/id/{id}")
  public RootEntityResponse<DtoAssets> getAssetsById(@PathVariable Long id) {
    Assets assets = assetsService.getAssetsById(id);
    DtoAssets dtoAssets = assetsMapper.toDtoAssets(assets);
    return ok(dtoAssets);
  }

  @Override
  @GetMapping("/{name}")
  public RootEntityResponse<DtoAssets> getAssetsByName(@PathVariable String name) {
    Assets assets = assetsService.getAssetsByName(name);
    DtoAssets dtoAssets = assetsMapper.toDtoAssets(assets);
    return ok(dtoAssets);
  }

}
