package com.cms.service.impl;

import com.cms.config.TenantContext;
import com.cms.dto.DtoStorageQuota;
import com.cms.entity.StorageQuota;
import com.cms.exception.QuotaExceededException;
import com.cms.repository.StorageQuotaRepository;
import com.cms.service.IStorageQuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageQuotaService implements IStorageQuotaService {

  private final StorageQuotaRepository repo;

  @Value("${app.storage.default-quota-bytes:3221225472}") // 3 GB
  private long defaultQuotaBytes;
  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  // FileService ile aynı kök (assets/t/{tenant})
  private static final String ASSETS_ROOT = "assets";
  private static final String TENANT_DIR = "t";

  private String currentTenant() {
    String t = TenantContext.getTenantId();
    if (t == null || t.isBlank()) {
      t = defaultTenant;
    }
    return t.replaceAll("[^a-zA-Z0-9_-]", "_");
  }

  private long effectiveLimit(StorageQuota q) {
    return (q != null && q.getLimitBytes() != null && q.getLimitBytes() > 0)
        ? q.getLimitBytes()
        : defaultQuotaBytes;
  }

  @Override
  @Transactional(readOnly = true)
  public void ensureWithin(long incomingBytes) {
    if (incomingBytes <= 0) {
      return;
    }
    String tenant = currentTenant();
    StorageQuota q = repo.findById(tenant).orElse(null);
    long used = (q != null) ? q.getUsedBytes() : 0L;
    long limit = effectiveLimit(q);
    if (used + incomingBytes > limit) {
      throw new QuotaExceededException(
          "Depolama kotası aşıldı (" + tenant + "): "
              + human(used) + " + " + human(incomingBytes) + " > " + human(limit));
    }
  }

  @Override
  @Transactional
  public void addUsage(long bytes) {
    if (bytes <= 0) {
      return;
    }
    String tenant = currentTenant();
    int updated = repo.addUsage(tenant, bytes);
    if (updated == 0) {
      // Satır yok → oluştur (race: başka thread oluşturduysa tekrar artır)
      try {
        repo.save(StorageQuota.builder().tenantId(tenant).usedBytes(bytes).build());
      } catch (DataIntegrityViolationException dup) {
        repo.addUsage(tenant, bytes);
      }
    }
  }

  @Override
  @Transactional
  public void removeUsage(long bytes) {
    if (bytes <= 0) {
      return;
    }
    repo.removeUsage(currentTenant(), bytes);
  }

  @Override
  @Transactional(readOnly = true)
  public DtoStorageQuota currentUsage() {
    String tenant = currentTenant();
    StorageQuota q = repo.findById(tenant)
        .orElse(StorageQuota.builder().tenantId(tenant).usedBytes(0L).build());
    return toDto(q);
  }

  @Override
  @Transactional
  public DtoStorageQuota setLimit(Long limitBytes) {
    String tenant = currentTenant();
    StorageQuota q = repo.findById(tenant)
        .orElseGet(() -> StorageQuota.builder().tenantId(tenant).usedBytes(0L).build());
    q.setLimitBytes(limitBytes);
    return toDto(repo.save(q));
  }

  @Override
  @Transactional
  public DtoStorageQuota recompute() {
    String tenant = currentTenant();
    long used = folderSize(Paths.get(ASSETS_ROOT, TENANT_DIR, tenant));
    StorageQuota q = repo.findById(tenant)
        .orElseGet(() -> StorageQuota.builder().tenantId(tenant).build());
    q.setUsedBytes(used);
    return toDto(repo.save(q));
  }

  private long folderSize(Path dir) {
    if (!Files.exists(dir)) {
      return 0L;
    }
    try (Stream<Path> stream = Files.walk(dir)) {
      return stream.filter(Files::isRegularFile).mapToLong(p -> {
        try {
          return Files.size(p);
        } catch (IOException e) {
          return 0L;
        }
      }).sum();
    } catch (IOException e) {
      log.warn("folderSize hesaplanamadı ({}): {}", dir, e.getMessage());
      return 0L;
    }
  }

  private DtoStorageQuota toDto(StorageQuota q) {
    long limit = effectiveLimit(q);
    double pct = limit > 0 ? (q.getUsedBytes() * 100.0 / limit) : 0.0;
    return DtoStorageQuota.builder()
        .tenantId(q.getTenantId())
        .usedBytes(q.getUsedBytes())
        .limitBytes(limit)
        .usedPercent(Math.round(pct * 10.0) / 10.0)
        .build();
  }

  private String human(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }
    String[] units = {"KB", "MB", "GB", "TB"};
    double v = bytes;
    int i = -1;
    do {
      v /= 1024.0;
      i++;
    } while (v >= 1024.0 && i < units.length - 1);
    return String.format("%.1f %s", v, units[i]);
  }
}
