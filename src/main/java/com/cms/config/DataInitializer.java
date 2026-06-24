package com.cms.config;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.repository.PermissionRepository;
import com.cms.repository.RoleRepository;
import com.cms.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Uygulama başladığında varsayılan roller, izinler ve
 * mevcut kullanıcılara SUPER_ADMIN rolü atar.
 * Sadece veritabanında ilgili veriler yoksa oluşturur (idempotent).
 *
 * <p>Kapalı varsayılan: {@code app.startup.rbac-seed=false}. DB hazırsa startup'ta çalışmaz.
 */
@Component
@ConditionalOnProperty(name = "app.startup.rbac-seed", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializer implements CommandLineRunner {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final UserRepository userRepository;
  private final DataSourceConfig.TenantDataSourceProperties tenantProperties;
  private final PasswordEncoder passwordEncoder;
  private final PlatformTransactionManager transactionManager;

  // İlk SUPER_ADMIN seed (her tenant'a, idempotent). Prod'da env ile override edilmeli.
  @Value("${SEED_ADMIN_USER:admin}")
  private String seedAdminUsername;
  @Value("${SEED_ADMIN_PASSWORD:Admin123!}")
  private String seedAdminPassword;
  @Value("${SEED_ADMIN_EMAIL:admin@elly.local}")
  private String seedAdminEmail;

  @Override
  public void run(String... args) {
    String originalTenant = TenantContext.getTenantId();
    TransactionTemplate tx = new TransactionTemplate(transactionManager);
    try {
      // basedb yok — HER tenant kendi permission/rol + (rolsüz değil) ilk SUPER_ADMIN'ini alır.
      // ÖNEMLİ: her tenant AYRI transaction'da seed edilir. Tek @Transactional ile bağlantı
      // ilk tenant'a pinlenir ve diğer tenant'lara YAZILAMAZdı (tenant mid-transaction değişmez).
      for (String tenantId : tenantProperties.getDatasources().keySet()) {
        try {
          TenantContext.setTenantId(tenantId);
          tx.executeWithoutResult(status -> {
            initializePermissions();
            initializeRoles();
            ensureSeedSuperAdmin();
          });
          log.info("✅ Tenant '{}' seed (permission+rol+super-admin) tamamlandı.", tenantId);
        } catch (Exception e) {
          log.warn("⚠️ Tenant '{}' seed başarısız (devam ediliyor): {}", tenantId, e.getMessage());
        }
      }
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }

  private void initializePermissions() {
    // PermissionConstants sınıfındaki tüm sabit alanları okuyarak permission oluştur
    Field[] fields = PermissionConstants.class.getDeclaredFields();
    int created = 0;

    for (Field field : fields) {
      if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
          && java.lang.reflect.Modifier.isFinal(field.getModifiers())
          && field.getType() == String.class) {
        try {
          String permissionName = (String) field.get(null);
          if (!permissionRepository.existsByName(permissionName)) {
            Permission permission = new Permission();
            permission.setName(permissionName);

            // Module: permission adının ":" öncesini büyük harfe çevir
            String module = permissionName.split(":")[0].toUpperCase();
            permission.setModule(module);

            // Description: field adından oluştur
            String description = field.getName().replace("_", " ");
            permission.setDescription(description);

            permissionRepository.save(permission);
            created++;
          }
        } catch (IllegalAccessException e) {
          log.warn("Permission okunamadı: {}", field.getName(), e);
        }
      }
    }

    if (created > 0) {
      log.info("✅ {} yeni permission oluşturuldu.", created);
    } else {
      log.info("ℹ️ Tüm permission'lar zaten mevcut.");
    }
  }

  private void initializeRoles() {
    List<Permission> allPermissions = permissionRepository.findAll();
    Set<Permission> allPermSet = new HashSet<>(allPermissions);

    // SUPER_ADMIN — tüm izinler
    syncRole("SUPER_ADMIN", "Tam yetki - tüm servislere erişim", allPermSet);

    // ADMIN — tüm izinler ama roles:* hariç. Users alanında TAM yetki
    // (users:read/update/manage). users:manage rol atamayı da açar ama assignRolesToUser
    // hiyerarşi guard'ı sayesinde ADMIN kendi seviyesinden (3) yüksek rol (SUPER_ADMIN=4) atayamaz.
    Set<Permission> adminPerms = new HashSet<>();
    for (Permission p : allPermissions) {
      if (!p.getName().startsWith("roles:")) {
        adminPerms.add(p);
      }
    }
    syncRole("ADMIN", "Panel yönetimi, kullanıcı yönetimi ve ayarlar", adminPerms);

    // EDITOR — içerik modülleri + mail/emails/email_templates/rabbit (tam)
    //           + cache/tenants (sadece read) + users (read+update)
    Set<Permission> editorPerms = new HashSet<>();
    Set<String> editorFullModules = Set.of(
        "POSTS", "PAGES", "COMPONENTS", "WIDGETS",
        "BANNERS", "ASSETS", "COMMENTS", "FORMS", "RATINGS", "CONTENTS", "BASIC_INFOS", "CHAT",
        "MAIL", "EMAILS", "EMAIL_TEMPLATES", "RABBIT");
    for (Permission p : allPermissions) {
      if (editorFullModules.contains(p.getModule())) {
        editorPerms.add(p);
      } else if (p.getModule().equals("CACHE") && p.getName().contains(":read")) {
        editorPerms.add(p);
      } else if (p.getName().equals("users:read") || p.getName().equals("users:update")) {
        editorPerms.add(p);
      }
    }
    syncRole("EDITOR", "İçerik oluşturma ve düzenleme", editorPerms);

    // VIEWER — tüm read izinleri + users:update (kendi /me profili güncellemesi için)
    Set<Permission> viewerPerms = new HashSet<>();
    for (Permission p : allPermissions) {
      if (p.getName().contains(":read") || p.getName().equals("users:update")) {
        viewerPerms.add(p);
      }
    }
    syncRole("VIEWER", "Sadece okuma yetkisi (kendi profilini güncelleyebilir)", viewerPerms);

    // TENANT — tenant-site kullanıcısı (public /auth/register ile oluşur). Panel'e GİREMEZ.
    // Tenant-site backend feature'ları "authenticated kullanıcı" check'i ile çalışır; RBAC
    // permission'larına ihtiyaç yok. Bu rol salt bir "tag" — login'de panel-rol filtrelemesi
    // bu sayede simetrik yapılır.
    syncRole("TENANT", "Tenant-site kullanıcısı (panel erişimi yok)", Set.of());
  }

  /**
   * Rol yoksa oluştur; varsa eksik permission'ları ekle (idempotent sync).
   * Bu sayede PermissionConstants'a eklenen yeni permission'lar uygulama
   * başladığında mevcut rollere de otomatik atanır.
   */
  private void syncRole(String name, String description, Set<Permission> expectedPermissions) {
    Optional<Role> existing = roleRepository.findByName(name);
    if (existing.isEmpty()) {
      Role role = new Role();
      role.setName(name);
      role.setDescription(description);
      role.setPermissions(expectedPermissions);
      roleRepository.save(role);
      log.info("✅ Rol oluşturuldu: {} ({} permission)", name, expectedPermissions.size());
      return;
    }

    Role role = existing.get();
    Set<Permission> current = role.getPermissions() != null ? role.getPermissions() : new HashSet<>();
    Set<Permission> missing = new HashSet<>();
    for (Permission p : expectedPermissions) {
      if (current.stream().noneMatch(c -> c.getName().equals(p.getName()))) {
        missing.add(p);
      }
    }

    if (!missing.isEmpty()) {
      current.addAll(missing);
      role.setPermissions(current);
      roleRepository.save(role);
      missing.forEach(p -> log.info("✅ {} rolüne permission eklendi: {}", name, p.getName()));
    } else {
      log.info("ℹ️ Rol sync'te değişiklik yok: {}", name);
    }
  }

  /**
   * MEVCUT tenant context'inde varsayılan SUPER_ADMIN kullanıcısını seed eder (idempotent).
   * Kullanıcı zaten varsa atlar. Krediler env'den:
   * {@code SEED_ADMIN_USER} / {@code SEED_ADMIN_PASSWORD} / {@code SEED_ADMIN_EMAIL}
   * (prod'da MUTLAKA override edilmeli — varsayılan şifre güvenli değildir).
   */
  private void ensureSeedSuperAdmin() {
    if (seedAdminUsername == null || seedAdminUsername.isBlank()) {
      return;
    }
    if (userRepository.existsByUsername(seedAdminUsername)) {
      log.info("ℹ️ Seed SUPER_ADMIN zaten mevcut: {}", seedAdminUsername);
      return;
    }
    Optional<Role> superAdminRole = roleRepository.findByName("SUPER_ADMIN");
    if (superAdminRole.isEmpty()) {
      log.warn("⚠️ SUPER_ADMIN rolü yok, seed admin oluşturulamadı.");
      return;
    }
    User admin = new User();
    admin.setUsername(seedAdminUsername);
    admin.setEmail(seedAdminEmail);
    admin.setPassword(passwordEncoder.encode(seedAdminPassword));
    admin.setProvider("local");
    admin.setIsActive(true);
    admin.setEmailVerified(true);
    admin.getRoles().add(superAdminRole.get());
    userRepository.save(admin);
    log.info("✅ Seed SUPER_ADMIN oluşturuldu: {}", seedAdminUsername);
  }
}
