package com.cms.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cms.config.TenantContext;
import com.cms.exception.BadRequestException;
import com.cms.service.IFileService;
import com.cms.service.IStorageQuotaService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileService implements IFileService {

  private final IStorageQuotaService storageQuotaService;

  @Value("${file.upload.directory:uploads/assets/images}")
  private String uploadDirectory;
  @Value("${file.upload.directory.files:uploads/assets}")
  private String uploadDirectoryFiles;
  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  /** Served + PVC kökü (WebConfig: /assets/** → file:assets/). Tenant izolasyonu bunun altında. */
  private static final String ASSETS_ROOT = "assets";
  private static final String TENANT_DIR = "t";

  /** Geçerli tenant'ı path-safe segment'e çevirir (null → basedb). Path traversal koruması. */
  private String currentTenant() {
    String t = TenantContext.getTenantId();
    if (t == null || t.isBlank()) {
      t = defaultTenant;
    }
    return t.replaceAll("[^a-zA-Z0-9_-]", "_");
  }

  /** Subfolder güvenliği: ".." temizliği + baş/son slash. */
  private String safeSub(String subfolder) {
    if (subfolder == null) {
      return "";
    }
    return subfolder.replace("\\", "/").replaceAll("\\.\\.", "").replaceAll("^/+|/+$", "");
  }

  /** Yalnız kota-izlenen (assets/t/{tenant}/...) dosya silinince kullanımı düşür. */
  private void untrackIfTenantScoped(String dbPath, long size) {
    if (size > 0 && dbPath != null && dbPath.startsWith(ASSETS_ROOT + "/" + TENANT_DIR + "/")) {
      storageQuotaService.removeUsage(size);
    }
  }

  @Override
  @Transactional
  public String saveImage(MultipartFile file, String subfolder) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("File is empty or null");
    }

    if (!isImageFile(file)) {
      throw new BadRequestException("File is not an image");
    }
    storageQuotaService.ensureWithin(file.getSize());

    try {
      // Tenant izolasyonu: assets/t/{tenant}/images/{subfolder}
      String tenant = currentTenant();
      String sub = safeSub(subfolder);
      Path uploadPath = Paths.get(ASSETS_ROOT, TENANT_DIR, tenant, "images");
      if (!sub.isEmpty()) {
        uploadPath = uploadPath.resolve(sub);
      }
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      String extension = getFileExtension(file.getOriginalFilename());
      String filename = UUID.randomUUID().toString() + (extension != null ? extension : "");

      Path filePath = uploadPath.resolve(filename);
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
      storageQuotaService.addUsage(file.getSize());

      // DB yolu = served root'a göre relative (= FS yolu)
      String dbPath = ASSETS_ROOT + "/" + TENANT_DIR + "/" + tenant + "/images";
      if (!sub.isEmpty()) {
        dbPath += "/" + sub;
      }
      dbPath += "/" + filename;
      return dbPath;
    } catch (IOException e) {
      throw new BadRequestException("Failed to save image file", e);
    }
  }

  @Override
  @Transactional
  public void deleteImage(String filePath) {
    if (filePath == null || filePath.isEmpty()) {
      return;
    }

    try {
      String actualPath = filePath.replace("assets/images", uploadDirectory);
      Path path = Paths.get(actualPath);
      long size = Files.exists(path) ? Files.size(path) : 0L;
      if (Files.exists(path)) {
        Files.delete(path);
      }
      untrackIfTenantScoped(filePath, size);
    } catch (IOException e) {
      throw new BadRequestException("Failed to delete image file", e);
    }
  }

  @Override
  @Transactional
  public String saveFile(MultipartFile file, String subfolder) {
    if (file == null || file.isEmpty()) {
      throw new BadRequestException("File is empty or null");
    }
    storageQuotaService.ensureWithin(file.getSize());

    try {
      // Tenant izolasyonu: assets/t/{tenant}/files/{subfolder}
      String tenant = currentTenant();
      String sub = safeSub(subfolder);
      Path uploadPath = Paths.get(ASSETS_ROOT, TENANT_DIR, tenant, "files");
      if (!sub.isEmpty()) {
        uploadPath = uploadPath.resolve(sub);
      }
      if (!Files.exists(uploadPath)) {
        Files.createDirectories(uploadPath);
      }

      String originalFilename = file.getOriginalFilename();
      Path filePath = uploadPath.resolve(originalFilename);
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
      storageQuotaService.addUsage(file.getSize());

      String dbPath = ASSETS_ROOT + "/" + TENANT_DIR + "/" + tenant + "/files";
      if (!sub.isEmpty()) {
        dbPath += "/" + sub;
      }
      dbPath += "/" + originalFilename;
      return dbPath;
    } catch (IOException e) {
      throw new BadRequestException("Failed to save file", e);
    }
  }

  @Override
  @Transactional
  public void deleteFile(String filePath) {
    try {
      String actualPath = filePath.replace("assets", uploadDirectoryFiles);
      Path path = Paths.get(actualPath);
      long size = Files.exists(path) ? Files.size(path) : 0L;
      if (Files.exists(path)) {
        Files.delete(path);
      }
      untrackIfTenantScoped(filePath, size);
    } catch (IOException e) {
      throw new BadRequestException("Failed to delete file", e);
    }
  }

  @Override
  public String getFileExtension(String filename) {
    try {
      if (filename != null && filename.contains(".")) {
        return filename.substring(filename.lastIndexOf("."));
      }
      return null;
    } catch (Exception e) {
      throw new BadRequestException("Failed to get file extension", e);
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
