package com.cms.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import com.cms.config.TenantContext;
import com.cms.config.UserAuthCacheService;
import com.cms.exception.BadRequestException;
import com.cms.service.IStorageQuotaService;

/**
 * Güvenlik kısa-vade paketi (S3/S7/T1/T2) birim testleri:
 * <ul>
 *   <li>S7/T1 — login brute-force sayaçları: tenant'lı anahtar, 5 hata sonrası kilit,
 *       başarılı girişte sıfırlama (local fallback yoluyla — Redis null).</li>
 *   <li>S1/S2 — upload uzantı allowlist: SVG/HTML reddi, geçerli uzantıda UUID isim.</li>
 *   <li>T2 — delete yolu assets kökü dışına çıkamaz.</li>
 * </ul>
 */
class SecurityPatch2Test {

  // =============== S7 + T1: login brute-force sayaçları ===============

  @Test
  void loginLockout_userCounter_fiveFailuresLockFifteenMinutes() {
    UserAuthCacheService svc = new UserAuthCacheService(null, null); // Redis yok → local fallback

    String tenant = "tenant1";
    String user = "bob@x.test";
    String ip = "203.0.113.10";

    for (int i = 0; i < 5; i++) {
      assertFalse(svc.isLoginLockedOut(tenant, user, ip), "ilk 5 denemede kilit OLMAMALI (deneme " + (i + 1) + ")");
      svc.recordFailedLoginAttempt(tenant, user, ip);
    }
    assertTrue(svc.isLoginLockedOut(tenant, user, ip), "5. hatadan sonra kilitlenMELİ");

    // Başarılı girişte sayaç sıfırlanır
    svc.clearLoginAttempts(tenant, user, ip);
    assertFalse(svc.isLoginLockedOut(tenant, user, ip), "başarılı girişte kilit kalkmalı");
  }

  @Test
  void loginLockout_tenantIsolated_sameUsernameDifferentTenant() {
    UserAuthCacheService svc = new UserAuthCacheService(null, null);

    // tenant1'deki bob kilitlenir
    for (int i = 0; i < 5; i++) {
      svc.recordFailedLoginAttempt("tenant1", "bob", "203.0.113.10");
    }
    assertTrue(svc.isLoginLockedOut("tenant1", "bob", "203.0.113.11"));

    // tenant2'deki aynı username etkilenmemeli (S7 tenant'lı anahtar)
    assertFalse(svc.isLoginLockedOut("tenant2", "bob", "203.0.113.10"));

    // Cache anahtarı da tenant'lı olmalı
    assertNotEquals(
        UserAuthCacheService.userCacheKey("tenant1", "admin"),
        UserAuthCacheService.userCacheKey("tenant2", "admin"));
  }

  @Test
  void loginLockout_ipCounter_thirtyFailuresLockAnyUser() {
    UserAuthCacheService svc = new UserAuthCacheService(null, null);
    String sprayIp = "198.51.100.7";
    for (int i = 0; i < UserAuthCacheService.MAX_LOGIN_FAILURES_PER_IP; i++) {
      // farklı username'lerle spray — kullanıcı sayacı dolmaz, IP sayacı dolmalı
      svc.recordFailedLoginAttempt("tenant1", "spray-user-" + i, sprayIp);
      // yeni kullanıcı adı her seferinde kilitli OLMAMALI (kullanıcı bazlı sayaç 1'er)
      assertFalse(svc.isLoginLockedOut("tenant1", "spray-user-" + i, "203.0.113.99"));
    }
    assertTrue(svc.isLoginLockedOut("tenant1", "fresh-victim", sprayIp),
        "aynı IP'den 30 hata sonrası yeni kullanıcı da kilitlenmeli");
  }

  // =============== S1/S2: upload uzantı allowlist + UUID isim ===============

  private FileService fileService() {
    IStorageQuotaService quota = mock(IStorageQuotaService.class);
    doNothing().when(quota).ensureWithin(anyLong());
    doNothing().when(quota).addUsage(anyLong());
    FileService svc = new FileService(quota);
    ReflectionTestUtils.setField(svc, "uploadDirectory", "assets/images");
    ReflectionTestUtils.setField(svc, "uploadDirectoryFiles", "assets");
    ReflectionTestUtils.setField(svc, "allowedExtensionsRaw",
        "jpg,jpeg,png,gif,webp,pdf,txt,csv,doc,docx,xls,xlsx,ppt,pptx,mp3,mp4,wav,webm,zip");
    return svc;
  }

  @BeforeAll
  static void setTenant() {
    TenantContext.setTenantId("testtenant");
  }

  @AfterAll
  static void clearTenant() {
    TenantContext.clear();
  }

  @AfterEach
  void cleanupTestAssets() throws Exception {
    Path testDir = Path.of("assets", "t", "testtenant");
    if (Files.exists(testDir)) {
      try (Stream<Path> walk = Files.walk(testDir)) {
        walk.sorted(Comparator.reverseOrder()).forEach(p -> {
          try {
            Files.delete(p);
          } catch (Exception ignored) {
            // test temizliği — sessiz geç
          }
        });
      }
    }
  }

  @Test
  void saveFile_rejectsActiveContentExtensions() {
    FileService svc = fileService();
    MultipartFile svg = new MockMultipartFile("f", "malicious.svg", "image/svg+xml", "<svg/>".getBytes());
    MultipartFile html = new MockMultipartFile("f", "payload.html", "text/html", "<script/>".getBytes());
    MultipartFile noExt = new MockMultipartFile("f", "noext", "application/pdf", "x".getBytes());

    assertThrows(BadRequestException.class, () -> svc.saveFile(svg, null), "SVG reddedilmeli");
    assertThrows(BadRequestException.class, () -> svc.saveFile(html, null), "HTML reddedilmeli");
    assertThrows(BadRequestException.class, () -> svc.saveFile(noExt, null), "uzantısız reddedilmeli");
  }

  @Test
  void saveFile_generatesUuidFilenameForAllowedExtension() {
    FileService svc = fileService();
    MultipartFile pdf = new MockMultipartFile("f", "../../etc/cron.d/evil.pdf", "application/pdf",
        "content".getBytes());

    String dbPath = svc.saveFile(pdf, null);

    // UUID isim: yol "../.." içermemeli ve orijinal dosya adı geçmemeli
    assertFalse(dbPath.contains(".."), "path traversal kalmamalı: " + dbPath);
    assertFalse(dbPath.contains("cron.d"), "orijinal dosya adı yolda olmamalı: " + dbPath);
    assertTrue(dbPath.endsWith(".pdf"), "izinli uzantı korunmalı: " + dbPath);
    String filename = dbPath.substring(dbPath.lastIndexOf('/') + 1);
    assertEquals(36 + ".pdf".length(), filename.length(), "UUID(36) + uzantı: " + filename);
  }

  @Test
  void saveImage_rejectsNonImageExtensionDespiteImageContentType() {
    FileService svc = fileService();
    // content-type image/* BEYANI svg dosyasını kurtaramamalı
    MultipartFile svgAsPng = new MockMultipartFile("f", "evil.svg", "image/png", "<svg/>".getBytes());
    assertThrows(BadRequestException.class, () -> svc.saveImage(svgAsPng, null));

    MultipartFile png = new MockMultipartFile("f", "photo.png", "image/png", "png".getBytes());
    String dbPath = svc.saveImage(png, null);
    assertTrue(dbPath.endsWith(".png"));
  }

  // =============== T2: delete containment ===============

  @Test
  void deleteFile_rejectsPathOutsideAssetsRoot() {
    FileService svc = fileService();
    assertThrows(BadRequestException.class, () -> svc.deleteFile("assets/../../application.properties"),
        "assets dışına çıkan yol reddedilmeli");
    assertThrows(BadRequestException.class, () -> svc.deleteFile("/etc/passwd"),
        "mutlak yol reddedilmeli");
    // Var olmayan ama assets altındaki yol — sessiz no-op (mevcut davranış korunur)
    svc.deleteFile("assets/t/testtenant/files/never-existed.pdf");
  }
}
