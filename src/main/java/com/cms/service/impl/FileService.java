package com.cms.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

  /**
   * Yüklenebilir dosya uzantıları (virgüllü, noktasız). Aktif içerik (svg/html/js/jsp/php/exe…)
   * KASITLI olarak yok: /assets/** anonim servis edildiği için bu uzantılar stored-XSS/RCE yüzeyi.
   */
  @Value("${file.upload.allowed-extensions:jpg,jpeg,png,gif,webp,pdf,txt,csv,doc,docx,xls,xlsx,ppt,pptx,mp3,mp4,wav,webm,zip}")
  private String allowedExtensionsRaw;

  /** Normalize edilmiş (trim + lowercase) allowlist. */
  private Set<String> allowedExtensionSet() {
    return java.util.Arrays.stream(allowedExtensionsRaw.split(","))
        .map(s -> s.trim().toLowerCase(Locale.ROOT))
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
  }

  /** Resim upload (saveImage) için izinli uzantılar — allowlist'in resim alt kümesi. */
  private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

  /** Served + PVC kökü (WebConfig: /assets/** → file:assets/). Tenant izolasyonu bunun altında. */
  private static final String ASSETS_ROOT = "assets";
  private static final String TENANT_DIR = "t";

  /** Geçerli tenant'ı path-safe segment'e çevirir. Context yoksa TENANT_REQUIRED (basedb yok). */
  private String currentTenant() {
    String t = TenantContext.getTenantId();
    if (t == null || t.isBlank()) {
      throw new com.cms.exception.TenantRequiredException(
          "Dosya işlemi için tenant context'i gerekli");
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
    // Uzantı da allowlist'ten geçmeli: content-type client beyanıdır; "a.svg" yerine
    // image/* beyanıyla SVG yüklenip /assets/**'ten stored-XSS olarak servis edilemez.
    String extension = requireAllowedExtension(file.getOriginalFilename(), IMAGE_EXTENSIONS);
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

      String filename = UUID.randomUUID().toString() + extension;

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

    // Güvenlik: dosya adı client kontrollüdür — ham isimle resolve() path traversal'a açıktır
    // ("../x" veya absolute path). Bu yüzden isim SUNUCUDA üretilir (UUID), yalnızca
    // allowlist'ten geçen uzantı korunur (saveImage ile aynı desen).
    String extension = requireAllowedExtension(file.getOriginalFilename(), allowedExtensionSet());

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

      String filename = UUID.randomUUID().toString() + extension;
      Path filePath = uploadPath.resolve(filename);
      Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
      storageQuotaService.addUsage(file.getSize());

      String dbPath = ASSETS_ROOT + "/" + TENANT_DIR + "/" + tenant + "/files";
      if (!sub.isEmpty()) {
        dbPath += "/" + sub;
      }
      dbPath += "/" + filename;
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

  /**
   * Uzantıyı çıkarır, lowercase normalize eder ve allowlist'e tabi tutar.
   *
   * @return nokta ile birlikte uzantı (örn. ".pdf") — allowlist'te güvenli
   */
  private String requireAllowedExtension(String originalFilename, Set<String> allowlist) {
    String extension = getFileExtension(originalFilename);
    if (extension == null) {
      throw new BadRequestException("File has no extension (allowed: "
          + String.join(",", allowlist) + ")");
    }
    String normalized = extension.substring(1).toLowerCase(Locale.ROOT);
    if (!allowlist.contains(normalized)) {
      throw new BadRequestException("File type not allowed: " + normalized
          + " (allowed: " + String.join(",", allowlist) + ")");
    }
    return "." + normalized;
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
