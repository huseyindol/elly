package com.cms.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.DataSourceConfig;
import com.cms.config.UserAuthCacheService;
import com.cms.config.TenantContext;
import com.cms.dto.DtoAdminUserUpdate;
import com.cms.dto.DtoChangePassword;
import com.cms.dto.DtoCreateUser;
import com.cms.dto.DtoUserPermissions;
import com.cms.dto.DtoUserResponse;
import com.cms.dto.DtoUserUpdate;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.exception.BadRequestException;
import com.cms.exception.ConflictException;
import com.cms.exception.TenantRequiredException;
import com.cms.mapper.UserMapper;
import com.cms.repository.RefreshTokenRepository;
import com.cms.repository.RoleRepository;
import com.cms.repository.UserRepository;
import com.cms.service.IAuthService;
import com.cms.service.IUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.cms.config.JwtAuthenticationFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements IUserService {

  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final UserAuthCacheService userAuthCacheService;
  private final IAuthService authService;
  private final DataSourceConfig.TenantDataSourceProperties tenantProperties;

  @Value("${app.frontend-url.panel:http://localhost:3001}")
  private String panelFrontendUrl;

  @Value("${app.frontend-url.tenant:http://localhost:3000}")
  private String tenantFrontendUrlFallback;

  @Override
  public DtoUserResponse getMe(String username) {
    User user = findUserByUsername(username);
    return userMapper.toDtoUserResponse(user);
  }

  @Override
  @Transactional
  public DtoUserResponse updateMe(String username, DtoUserUpdate dto) {
    User user = findUserByUsername(username);

    // Username benzersizlik kontrolü
    if (dto.getUsername() != null && !dto.getUsername().isBlank()
        && !dto.getUsername().equals(user.getUsername())
        && userRepository.existsByUsername(dto.getUsername())) {
      throw new ConflictException("Username already exists: " + dto.getUsername());
    }

    // Email benzersizlik kontrolü
    if (dto.getEmail() != null && !dto.getEmail().isBlank()
        && !dto.getEmail().equals(user.getEmail())
        && userRepository.existsByEmail(dto.getEmail())) {
      throw new ConflictException("Email already exists: " + dto.getEmail());
    }

    // MapStruct ile sadece null olmayan alanları güncelle
    userMapper.updateUserFromDto(dto, user);

    User savedUser = userRepository.save(user);
    // Profil güncellendiğinde cache temizle (username/email değişmiş olabilir)
    userAuthCacheService.evictUserCache(username);
    if (!username.equals(savedUser.getUsername())) {
      userAuthCacheService.evictUserCache(savedUser.getUsername());
    }
    log.info("User profile updated: {}", savedUser.getUsername());
    return userMapper.toDtoUserResponse(savedUser);
  }

  @Override
  @Transactional
  public void changePassword(String username, DtoChangePassword dto) {
    User user = findUserByUsername(username);

    // OAuth kullanıcıları şifre değiştiremez
    if (!"local".equals(user.getProvider())) {
      throw new BadRequestException("Password change is not available for OAuth users");
    }

    // Mevcut şifre doğrulaması
    if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
      throw new BadRequestException("Current password is incorrect");
    }

    // Yeni şifre aynı olmamalı
    if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
      throw new BadRequestException("New password must be different from current password");
    }

    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    userRepository.save(user);
    // Şifre değiştiğinde cache temizle
    userAuthCacheService.evictUserCache(username);
    log.info("Password changed for user: {}", username);
  }

  @Override
  public DtoUserPermissions getMyPermissions(String username) {
    // SecurityContext'ten CachedUserPrincipal al — Redis cache'ten geliyor, DB'ye gitmez
    Set<String> authorities = getAuthoritiesFromContext();

    if (authorities == null || authorities.isEmpty()) {
      return DtoUserPermissions.builder()
          .roles(List.of())
          .permissions(List.of())
          .permissionsByModule(Map.of())
          .build();
    }

    // ROLE_ prefix'li olanları ayır
    List<String> roles = authorities.stream()
        .filter(a -> a.startsWith("ROLE_"))
        .map(a -> a.substring(5)) // "ROLE_SUPER_ADMIN" → "SUPER_ADMIN"
        .sorted()
        .collect(Collectors.toList());

    // Permission'ları ayır (modül:işlem formatında olanlar)
    List<String> permissions = authorities.stream()
        .filter(a -> !a.startsWith("ROLE_") && a.contains(":"))
        .sorted()
        .collect(Collectors.toList());

    // Modüle göre grupla: {"posts": ["create","read"], "pages": ["read","update"]}
    Map<String, List<String>> permissionsByModule = new HashMap<>();
    for (String perm : permissions) {
      String[] parts = perm.split(":", 2);
      permissionsByModule.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
    }

    return DtoUserPermissions.builder()
        .roles(roles)
        .permissions(permissions)
        .permissionsByModule(permissionsByModule)
        .build();
  }

  // Panel ↔ tenant ayrımı (login filtresiyle simetrik). Frontend "audience" parametresiyle
  // doğru tabı doğru kullanıcılarla doldurur.
  private static final Set<String> PANEL_ROLE_NAMES =
      Set.of("SUPER_ADMIN", "ADMIN", "EDITOR", "VIEWER");
  private static final String TENANT_ROLE_NAME = "TENANT";

  @Override
  public List<DtoUserResponse> getAllUsers(String audience) {
    List<User> users = userRepository.findAll();
    java.util.function.Predicate<User> filter;
    if ("panel".equalsIgnoreCase(audience)) {
      filter = u -> u.getRoles() != null
          && u.getRoles().stream().anyMatch(r -> PANEL_ROLE_NAMES.contains(r.getName()));
    } else if ("tenant".equalsIgnoreCase(audience)) {
      filter = u -> u.getRoles() != null
          && u.getRoles().stream().anyMatch(r -> TENANT_ROLE_NAME.equals(r.getName()));
    } else {
      filter = u -> true;
    }
    return users.stream()
        .filter(filter)
        .map(userMapper::toDtoUserResponse)
        .collect(Collectors.toList());
  }

  @Override
  public DtoUserResponse getUserById(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new BadRequestException("User not found with id: " + id));
    return userMapper.toDtoUserResponse(user);
  }

  @Override
  @Transactional
  public DtoUserResponse createUser(DtoCreateUser request) {
    // Çağıran user authenticated SUPER_ADMIN/ADMIN — JwtTenantFilter context'i zaten kurmuş.
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new TenantRequiredException("Tenant context bos — admin create icin gerekli");
    }

    if (userRepository.existsByEmail(request.getEmail())) {
      throw new ConflictException("Email already exists: " + request.getEmail());
    }
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new ConflictException("Username already exists: " + request.getUsername());
    }

    // audience: "panel" (default) → panel kullanıcısı; "tenant" → tenant-site kullanıcısı.
    boolean isTenantAudience = "tenant".equalsIgnoreCase(request.getAudience());
    if (request.getAudience() != null && !request.getAudience().isBlank()
        && !isTenantAudience && !"panel".equalsIgnoreCase(request.getAudience())) {
      throw new BadRequestException("audience 'panel' veya 'tenant' olmalı");
    }

    Set<Role> rolesToAssign = new HashSet<>();
    if (isTenantAudience) {
      // Tenant kullanıcısı: TENANT rolü zorunlu ve tek — rol seçimi yok.
      if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
        throw new BadRequestException("Tenant kullanıcısına rol atanamaz (TENANT otomatik)");
      }
      Role tenantRole = roleRepository.findByName("TENANT")
          .orElseThrow(() -> new BadRequestException("TENANT rolü tanımlı değil (seed eksik)"));
      rolesToAssign.add(tenantRole);
    } else if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
      // Hiyerarşi guard'ı (assignRolesToUser ile aynı kural): atanacak rol, çağıranın
      // kendi seviyesini geçemez — ADMIN'in SUPER_ADMIN kullanıcı yaratması engellenir.
      int requesterLevel = currentUserRoleLevel();
      for (Long roleId : request.getRoleIds()) {
        Role r = roleRepository.findById(roleId)
            .orElseThrow(() -> new BadRequestException("Role not found: " + roleId));
        // TENANT rolü panel-audience yolundan atanamaz — tenant kullanıcısı audience=tenant ile.
        if ("TENANT".equals(r.getName())) {
          throw new BadRequestException("TENANT rolü panel kullanıcısına atanamaz");
        }
        if (roleLevel(r.getName()) > requesterLevel) {
          throw new com.cms.exception.ForbiddenException(
              "Cannot assign role '" + r.getName() + "': exceeds your own permission level");
        }
        rolesToAssign.add(r);
      }
    } else {
      // Default: ADMIN rolü atanır (panel-create varsayılan davranışı).
      Role admin = roleRepository.findByName("ADMIN")
          .orElseThrow(() -> new BadRequestException("ADMIN rolü tanımlı değil"));
      rolesToAssign.add(admin);
    }

    String verificationToken = UUID.randomUUID().toString();

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    user.setFirstName(request.getFirstName());
    user.setLastName(request.getLastName());
    user.setProvider("local");
    user.setIsActive(true);
    user.setEmailVerified(false);
    user.setEmailVerificationToken(verificationToken);
    user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));
    user.getRoles().addAll(rolesToAssign);

    User saved = userRepository.save(user);

    // Verification mail: panel kullanıcısı → PANEL frontend URL; tenant kullanıcısı →
    // o tenant'ın kendi frontend-url'i (app.tenants.datasources.{tid}.frontend-url,
    // yoksa global fallback) — kullanıcı kendi domain'inden doğrular.
    String mailFrontendUrl;
    if (isTenantAudience) {
      String tenantUrl = tenantProperties.getDatasources().get(tenantId).getFrontendUrl();
      mailFrontendUrl = (tenantUrl != null && !tenantUrl.isBlank())
          ? tenantUrl
          : tenantFrontendUrlFallback;
    } else {
      mailFrontendUrl = panelFrontendUrl;
    }
    authService.sendVerificationEmail(saved, tenantId, verificationToken, mailFrontendUrl);

    log.info("Kullanici olusturuldu (panel): id={} username={} tenant={} audience={}",
        saved.getId(), saved.getUsername(), tenantId, isTenantAudience ? "tenant" : "panel");
    return userMapper.toDtoUserResponse(saved);
  }

  @Override
  @Transactional
  public DtoUserResponse updateUser(Long id, DtoAdminUserUpdate request) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new BadRequestException("User not found with id: " + id));
    String oldUsername = user.getUsername();

    // Username değişiyorsa unique kontrol
    if (request.getUsername() != null && !request.getUsername().isBlank()
        && !request.getUsername().equals(user.getUsername())) {
      if (userRepository.existsByUsername(request.getUsername())) {
        throw new ConflictException("Username already exists: " + request.getUsername());
      }
      user.setUsername(request.getUsername());
    }

    // Email değişiyorsa unique kontrol
    if (request.getEmail() != null && !request.getEmail().isBlank()
        && !request.getEmail().equals(user.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw new ConflictException("Email already exists: " + request.getEmail());
      }
      user.setEmail(request.getEmail());
    }

    if (request.getFirstName() != null) {
      user.setFirstName(request.getFirstName());
    }
    if (request.getLastName() != null) {
      user.setLastName(request.getLastName());
    }

    // Admin şifre sıfırlaması — eski şifre sorulmaz; açık oturumlar sonlandırılır
    // (refresh token cleanup; access token kalan ömrünü cache evict sonrası DB'den
    // yüklenen tokenVersion kontrolü zaten sınırlar).
    boolean passwordReset = request.getNewPassword() != null && !request.getNewPassword().isBlank();
    if (passwordReset) {
      if (!"local".equals(user.getProvider())) {
        throw new BadRequestException("Password reset is not available for OAuth users");
      }
      user.setPassword(passwordEncoder.encode(request.getNewPassword()));
      refreshTokenRepository.deleteByUserId(id);
    }

    User saved = userRepository.save(user);

    // Auth cache evict — eski VE yeni username anahtarları (username değişmiş olabilir)
    if (oldUsername != null) {
      userAuthCacheService.evictUserCache(oldUsername);
    }
    if (saved.getUsername() != null && !saved.getUsername().equals(oldUsername)) {
      userAuthCacheService.evictUserCache(saved.getUsername());
    }

    log.info("Kullanici guncellendi (admin): id={} username={} passwordReset={}",
        id, saved.getUsername(), passwordReset);
    return userMapper.toDtoUserResponse(saved);
  }

  @Override
  @Transactional
  public void deleteUser(Long id, String currentUsername) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new BadRequestException("User not found with id: " + id));

    // Self-delete koruması — admin kendi hesabını silemez.
    if (user.getUsername() != null && user.getUsername().equals(currentUsername)) {
      throw new BadRequestException("Kendi hesabınızı silemezsiniz");
    }

    // RefreshToken @ManyToOne(User) ile FK constraint'i taşır → user silinmeden önce temizlenmeli.
    // Notification/ChatMessage gibi userId Long alanları FK taşımaz → orphan kalır (tarih korunur).
    refreshTokenRepository.deleteByUserId(id);

    String username = user.getUsername();
    userRepository.delete(user);

    // Redis auth cache invalidate — silinen kullanıcının cache'lenmiş oturumu kalmasın
    if (username != null) {
      userAuthCacheService.evictUserCache(username);
    }
    log.info("Kullanici silindi: id={} username={} silen={}", id, username, currentUsername);
  }

  @Override
  @Transactional
  public DtoUserResponse setUserStatus(Long id, boolean isActive, String currentUsername) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new BadRequestException("User not found with id: " + id));

    // Self-deactivate koruması — admin kendi hesabını pasifleştirip kilitleyemez.
    if (!isActive && user.getUsername() != null && user.getUsername().equals(currentUsername)) {
      throw new BadRequestException("Kendi hesabınızı pasifleştiremezsiniz");
    }

    user.setIsActive(isActive);
    User saved = userRepository.save(user);

    // Pasifleştirme sonrası açık oturumlar mantıksal olarak sona ermeli:
    // (a) auth cache invalidate → bir sonraki request DB'den okur, isActive=false → redirected
    // (b) refresh token sil → silent refresh denerse fail; tam logout
    if (!isActive) {
      refreshTokenRepository.deleteByUserId(id);
    }
    if (saved.getUsername() != null) {
      userAuthCacheService.evictUserCache(saved.getUsername());
    }
    log.info("Kullanici durumu degisti: id={} username={} isActive={} degisen={}",
        id, saved.getUsername(), isActive, currentUsername);
    return userMapper.toDtoUserResponse(saved);
  }

  // =============== Helpers ===============

  /**
   * SecurityContext'ten mevcut kullanıcının authority'lerini al.
   * JwtAuthenticationFilter tarafından set edilmiş CachedUserPrincipal üzerinden gelir.
   */
  private Set<String> getAuthoritiesFromContext() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticationFilter.CachedUserPrincipal principal) {
      return principal.getCachedUser().getAuthorities();
    }
    return Set.of();
  }

  private User findUserByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new BadRequestException("User not found: " + username));
  }

  /** Çağıranın en yüksek rol seviyesi (SecurityContext'ten). SUPER_ADMIN=4, ADMIN=3, EDITOR=2, diğer=1. */
  private int currentUserRoleLevel() {
    Set<String> authorities = getAuthoritiesFromContext();
    if (authorities.contains("ROLE_SUPER_ADMIN")) return 4;
    if (authorities.contains("ROLE_ADMIN")) return 3;
    if (authorities.contains("ROLE_EDITOR")) return 2;
    return 1;
  }

  private static int roleLevel(String roleName) {
    return switch (roleName) {
      case "SUPER_ADMIN" -> 4;
      case "ADMIN" -> 3;
      case "EDITOR" -> 2;
      default -> 1; // VIEWER, TENANT vb.
    };
  }
}
