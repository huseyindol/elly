package com.cms.service;

import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
  String saveImage(MultipartFile file, String subfolder);

  void deleteImage(String filePath);

  boolean isImageFile(MultipartFile file);
}
