package com.cms.service;

import org.springframework.web.multipart.MultipartFile;

public interface IFileService {
  String saveImage(MultipartFile file, String subfolder);

  void deleteImage(String filePath);

  String saveFile(MultipartFile file, String subfolder);

  void deleteFile(String filePath);

  String getFileExtension(String filename);

  boolean isImageFile(MultipartFile file);
}
