package com.cms.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cms.service.IFileService;

@Service
public class FileService implements IFileService {

  @Value("${file.upload.directory:uploads/assets/images}")
  private String uploadDirectory;

  @Override
  public String saveImage(MultipartFile file, String subfolder) {
    if (file == null || file.isEmpty()) {
      throw new IllegalArgumentException("File is empty or null");
    }

    if (!isImageFile(file)) {
      throw new IllegalArgumentException("File is not an image");
    }

    try {
      // Klasör yolu oluştur
      Path uploadPath = Paths.get(uploadDirectory, subfolder != null ? subfolder : "");

      // Klasör yoksa oluştur
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      // Benzersiz dosya adı oluştur
      String originalFilename = file.getOriginalFilename();
      String extension = "";
      if (originalFilename != null && originalFilename.contains(".")) {
        extension = originalFilename.substring(originalFilename.lastIndexOf("."));
      }
      String filename = UUID.randomUUID().toString() + extension;

      // Dosyayı kaydet
      Path filePath = uploadPath.resolve(filename);
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

      // DB'de saklanacak yol: assets/images/{subfolder}/{filename}
      String dbPath = "assets/images";
      if (subfolder != null && !subfolder.isEmpty()) {
        dbPath += "/" + subfolder;
      }
      dbPath += "/" + filename;

      return dbPath;
    } catch (IOException e) {
      throw new RuntimeException("Failed to save image file", e);
    }
  }

  @Override
  public void deleteImage(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return;
    }

    try {
      // DB'deki yol: assets/images/... -> uploads/assets/images/...
      String actualPath = filePath.replace("assets/images", uploadDirectory);
      Path path = Paths.get(actualPath);

      if (Files.exists(path)) {
        Files.delete(path);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to delete image file", e);
    }
  }

  @Override
  public boolean isImageFile(MultipartFile file) {
    if (file == null) {
      return false;
    }

    String contentType = file.getContentType();
    if (contentType == null) {
      return false;
    }

    return contentType.startsWith("image/");
  }
}
