package com.cms.config;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.repository.PermissionRepository;
import com.cms.repository.RoleRepository;
import com.cms.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * Uygulama başladığında varsayılan roller, izinler ve
 * mevcut kullanıcılara SUPER_ADMIN rolü atar.
 * Sadece veritabanında ilgili veriler yoksa oluşturur (idempotent).
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DataInitializer implements CommandLineRunner {

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final UserRepository userRepository;

  @Value("${app.tenants.default-tenant:basedb}")
  private String defaultTenant;

  @Override
  @Transactional
  public void run(String... args) {
    String originalTenant = TenantContext.getTenantId();
    try {
      TenantContext.setTenantId(defaultTenant);
      initializePermissions();
      initializeRoles();
      assignSuperAdminToExistingUsers();
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

    // ADMIN — tüm izinler ama users:manage ve roles:* hariç
    Set<Permission> adminPerms = new HashSet<>();
    for (Permission p : allPermissions) {
      if (!p.getName().startsWith("roles:") && !p.getName().equals("users:manage")) {
        adminPerms.add(p);
      }
    }
    syncRole("ADMIN", "Panel yönetimi - içerik ve ayarlar", adminPerms);

    // EDITOR — içerik modülleri (CRUD), mail-cache-tenant hariç
    Set<Permission> editorPerms = new HashSet<>();
    Set<String> editorModules = Set.of("POSTS", "PAGES", "COMPONENTS", "WIDGETS",
        "BANNERS", "ASSETS", "COMMENTS", "FORMS", "RATINGS", "CONTENTS", "BASIC_INFOS");
    for (Permission p : allPermissions) {
      if (editorModules.contains(p.getModule())) {
        editorPerms.add(p);
      }
    }
    syncRole("EDITOR", "İçerik oluşturma ve düzenleme", editorPerms);

    // VIEWER — sadece read izinleri
    Set<Permission> viewerPerms = new HashSet<>();
    for (Permission p : allPermissions) {
      if (p.getName().contains(":read")) {
        viewerPerms.add(p);
      }
    }
    syncRole("VIEWER", "Sadece okuma yetkisi", viewerPerms);
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

  private void assignSuperAdminToExistingUsers() {
    Optional<Role> superAdminRole = roleRepository.findByName("SUPER_ADMIN");
    if (superAdminRole.isEmpty()) {
      log.warn("⚠️ SUPER_ADMIN rolü bulunamadı, kullanıcılara rol atanamadı.");
      return;
    }

    List<User> usersWithoutRoles = userRepository.findAll().stream()
        .filter(u -> u.getRoles() == null || u.getRoles().isEmpty())
        .toList();

    if (!usersWithoutRoles.isEmpty()) {
      Role role = superAdminRole.get();
      for (User user : usersWithoutRoles) {
        user.setRoles(new HashSet<>(Set.of(role)));
        userRepository.save(user);
        log.info("✅ SUPER_ADMIN rolü atandı: {}", user.getUsername());
      }
    } else {
      log.info("ℹ️ Tüm kullanıcıların zaten rolü mevcut.");
    }
  }
}
