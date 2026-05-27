package com.cms.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.CustomUserDetailsService;
import com.cms.config.DataSourceConfig;
import com.cms.config.UserAuthCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoMfaDisableRequest;
import com.cms.dto.DtoMfaSetupResponse;
import com.cms.dto.DtoMfaSetupVerifyRequest;
import com.cms.dto.DtoMfaVerifyRequest;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.dto.DtoTenantTokenResponse;
import com.cms.dto.EmailRequest;
import com.cms.service.TotpService;
import com.cms.util.AesEncryptor;
import com.cms.entity.RefreshToken;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.exception.BadRequestException;
import com.cms.exception.ConflictException;
import com.cms.exception.ForbiddenException;
import com.cms.exception.UnauthorizedException;
import com.cms.repository.MailAccountRepository;
import com.cms.repository.RefreshTokenRepository;
import com.cms.repository.RoleRepository;
import com.cms.repository.UserRepository;
import com.cms.service.IAuthService;
import com.cms.service.IEmailService;
import com.cms.util.JwtUtil;
import com.cms.util.UserUtil;
import com.cms.config.TenantContext;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final UserUtil userUtil;
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenRepository refreshTokenRepository;
  private final DataSourceConfig.TenantDataSourceProperties tenantProperties;
  private final UserAuthCacheService userAuthCacheService;
  private final RoleRepository roleRepository;
  private final MailAccountRepository mailAccountRepository;
  private final IEmailService emailService;
  private final TotpService totpService;
  private final AesEncryptor aesEncryptor;

  @Value("${jwt.refresh.expiration:3600000}")
  private Long refreshTokenExpiration;
  @Value("${jwt.expiration:1800000}")
  private Long accessTokenExpiration;
  @Value("${app.frontend.url:http://localhost:3000}")
  private String frontendUrl;

  @Override
  @Transactional
  public DtoAuthResponse register(DtoRegister dtoRegister) {
    String tenantId = dtoRegister.getTenantId();
    String originalTenant = TenantContext.getTenantId();

    // Tenant doğrulama ve context switch
    if (tenantId != null && !tenantId.isBlank()) {
      if (!tenantProperties.getDatasources().containsKey(tenantId)) {
        throw new BadRequestException("Tenant not found: " + tenantId);
      }
      TenantContext.setTenantId(tenantId);
    }

    try {
      // Email ve username kontrolü
      if (userRepository.existsByEmail(dtoRegister.getEmail())) {
        throw new ConflictException("Email already exists: " + dtoRegister.getEmail());
      }
      if (userRepository.existsByUsername(dtoRegister.getUsername())) {
        throw new ConflictException("Username already exists: " + dtoRegister.getUsername());
      }

      // VIEWER rolünü bul (tenant DB'de olmayabilir, graceful fallback)
      Role viewerRole = roleRepository.findByName("VIEWER").orElse(null);

      // Verification token oluştur
      String verificationToken = UUID.randomUUID().toString();

      // Yeni kullanıcı oluştur
      User user = new User();
      user.setUsername(dtoRegister.getUsername());
      user.setEmail(dtoRegister.getEmail());
      user.setPassword(passwordEncoder.encode(dtoRegister.getPassword()));
      user.setFirstName(dtoRegister.getFirstName());
      user.setLastName(dtoRegister.getLastName());
      user.setProvider("local");
      user.setIsActive(true);
      user.setEmailVerified(false);
      user.setEmailVerificationToken(verificationToken);
      user.setVerificationTokenExpiresAt(LocalDateTime.now().plusHours(24));

      if (viewerRole != null) {
        user.getRoles().add(viewerRole);
      }

      // managedTenants sadece SUPER_ADMIN tarafından set edilebilir
      // Public register endpoint'inden gelen isteklerde bu alan yok sayılır
      if (dtoRegister.getManagedTenants() != null && !dtoRegister.getManagedTenants().isEmpty()) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth != null && auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (isSuperAdmin) {
          user.setManagedTenants(dtoRegister.getManagedTenants());
        }
        // SUPER_ADMIN değilse sessizce yok say — hata fırlatma, kayıt devam eder
      }

      User savedUser = userRepository.save(user);

      // Doğrulama e-postası gönder (hata olursa kayıt yine tamamlanır)
      sendVerificationEmail(savedUser, tenantId, verificationToken);

      DtoAuthResponse response = new DtoAuthResponse();
      response.setUserId(savedUser.getId());
      response.setUsername(savedUser.getUsername());
      response.setEmail(savedUser.getEmail());
      response.setMessage("Kayıt başarılı. Lütfen e-posta adresinize gönderilen doğrulama bağlantısına tıklayın.");

      return response;
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }

  @Override
  @Transactional
  public void verifyEmail(String token, String tenantId) {
    String originalTenant = TenantContext.getTenantId();

    if (tenantId != null && !tenantId.isBlank()) {
      if (!tenantProperties.getDatasources().containsKey(tenantId)) {
        throw new BadRequestException("Tenant not found: " + tenantId);
      }
      TenantContext.setTenantId(tenantId);
    }

    try {
      User user = userRepository.findByEmailVerificationToken(token)
          .orElseThrow(() -> new BadRequestException("Geçersiz veya süresi dolmuş doğrulama bağlantısı"));

      if (Boolean.TRUE.equals(user.getEmailVerified())) {
        throw new BadRequestException("E-posta adresi zaten doğrulanmış");
      }

      if (user.getVerificationTokenExpiresAt() != null
          && LocalDateTime.now().isAfter(user.getVerificationTokenExpiresAt())) {
        throw new BadRequestException("Doğrulama bağlantısının süresi dolmuş. Lütfen yeniden kayıt olun.");
      }

      user.setEmailVerified(true);
      user.setEmailVerificationToken(null);
      user.setVerificationTokenExpiresAt(null);
      userRepository.save(user);

      log.info("E-posta doğrulandı: {} (tenant: {})", user.getEmail(), tenantId);
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }

  private void sendVerificationEmail(User user, String tenantId, String verificationToken) {
    // Mail hesapları admin panelde (basedb) yönetilir.
    // Tenant context geçici olarak basedb'ye alınır, sonra geri döner.
    String currentTenant = TenantContext.getTenantId();
    try {
      TenantContext.setTenantId("basedb");

      var mailAccount = (tenantId != null && !tenantId.isBlank())
          ? mailAccountRepository.findByTenantIdAndIsPrimaryTrueAndActiveTrue(tenantId)
          : java.util.Optional.<com.cms.entity.MailAccount>empty();

      if (mailAccount.isEmpty()) {
        log.warn("Doğrulama e-postası gönderilemedi — basedb'de '{}' tenant'ına ait aktif primary mail hesabı yok", tenantId);
        return;
      }

      String verifyUrl = org.springframework.web.util.UriComponentsBuilder
          .fromUriString(frontendUrl)
          .path("/verify-email")
          .queryParam("token", verificationToken)
          .queryParam("tenantId", tenantId != null ? tenantId : "")
          .build().toUriString();

      EmailRequest emailRequest = new EmailRequest();
      emailRequest.setTo(user.getEmail());
      emailRequest.setSubject("E-posta Adresinizi Doğrulayın");
      emailRequest.setTemplateName("email-verification");
      emailRequest.setMailAccountId(mailAccount.get().getId());
      emailRequest.setDynamicData(Map.of(
          "name", (user.getFirstName() != null ? user.getFirstName() : user.getUsername()),
          "verifyUrl", verifyUrl));

      emailService.sendEmail(emailRequest);
    } catch (Exception e) {
      log.error("Doğrulama e-postası gönderilemedi: {} — {}", user.getEmail(), e.getMessage());
    } finally {
      TenantContext.setTenantId(currentTenant);
    }
  }

  @Override
  @Transactional
  public DtoAuthResponse login(DtoLogin dtoLogin) {
    String tenantId = dtoLogin.getTenantId();
    boolean isAdminLogin = "admin".equalsIgnoreCase(dtoLogin.getLoginType());

    // Tenant varlık kontrolü
    if (tenantId != null && !tenantId.isBlank()) {
      if (!tenantProperties.getDatasources().containsKey(tenantId)) {
        throw new BadRequestException("Tenant not found: " + tenantId);
      }
    }

    // Site login: authentication tenant DB'sinde yapılır
    if (!isAdminLogin && tenantId != null && !tenantId.isBlank()) {
      TenantContext.setTenantId(tenantId);
    }

    // Authentication işlemi (BCrypt - kasıtlı yavaş)
    Authentication authentication;
    try {
      authentication = authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(
              dtoLogin.getUsernameOrEmail(),
              dtoLogin.getPassword()));
    } catch (org.springframework.security.core.AuthenticationException e) {
      throw new UnauthorizedException("Invalid username/email or password");
    }

    // User bilgilerini authentication'dan al (tekrar sorgu yapmadan)
    CustomUserDetailsService.CustomUserPrincipal principal = (CustomUserDetailsService.CustomUserPrincipal) authentication
        .getPrincipal();
    User user = principal.getUser();

    // Kullanıcı aktif mi kontrol et
    if (!user.getIsActive()) {
      throw new BadRequestException("User account is deactivated");
    }

    // E-posta doğrulama kontrolü (tenant login için)
    if (!isAdminLogin && !Boolean.TRUE.equals(user.getEmailVerified())) {
      throw new BadRequestException("E-posta adresiniz henüz doğrulanmamış. Lütfen e-postanızı kontrol edin.");
    }

    // Admin login: tenant yetki kontrolü (managedTenants)
    if (isAdminLogin && tenantId != null && !tenantId.isBlank()) {
      if (user.getManagedTenants() == null || !user.getManagedTenants().contains(tenantId)) {
        throw new ForbiddenException("User is not authorized for tenant: " + tenantId);
      }
    }

    String loginSource = isAdminLogin ? "admin" : "tenant";

    // ── 2FA kontrolü ────────────────────────────────────────────────────
    // Şifre doğrulandı; MFA aktifse kısa ömürlü mfaToken dön, JWT henüz yok.
    // tokenVersion bu adımda artırılmaz — MFA doğrulandığında artırılır.
    if (Boolean.TRUE.equals(user.getMfaEnabled()) && Boolean.TRUE.equals(user.getMfaSetupVerified())) {
      String mfaToken = jwtUtil.generateMfaToken(user.getId(), loginSource, tenantId);
      DtoAuthResponse mfaResponse = new DtoAuthResponse();
      mfaResponse.setMfaRequired(true);
      mfaResponse.setMfaToken(mfaToken);
      return mfaResponse;
    }

    return issueFullAuthToken(user, loginSource, tenantId);
  }

  /**
   * Kullanıcı için access + refresh token üretir ve DtoAuthResponse döner.
   * login() ve verifyMfaLogin() tarafından paylaşılır.
   */
  private DtoAuthResponse issueFullAuthToken(User user, String loginSource, String tenantId) {
    // Token version'ı artır
    Long newTokenVersion = (user.getTokenVersion() != null ? user.getTokenVersion() : 0L) + 1;

    // Native query ile token_version'ı güncelle (tam entity update yerine)
    userRepository.updateTokenVersion(user.getId(), newTokenVersion);
    // Token version değişti, cache'i temizle
    userAuthCacheService.evictUserCache(user.getUsername());

    // JWT token oluştur (güncel version ile)
    String token = jwtUtil.generateToken(user.getId(), user.getUsername(), newTokenVersion, tenantId, loginSource);

    // Refresh token oluştur
    String refreshTokenString = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), tenantId, loginSource);

    // Refresh token'ı upsert (INSERT veya UPDATE - tek sorgu)
    Date expiryDate = new Date(System.currentTimeMillis() + refreshTokenExpiration);
    refreshTokenRepository.upsertRefreshToken(user.getId(), refreshTokenString, expiryDate);

    // Response oluştur
    DtoAuthResponse response = new DtoAuthResponse();
    response.setToken(token);
    response.setRefreshToken(refreshTokenString);
    response.setUserId(user.getId());
    response.setUsername(user.getUsername());
    response.setEmail(user.getEmail());
    response.setUserCode(userUtil.generateUserCode(user));
    response.setExpiredDate(System.currentTimeMillis() + accessTokenExpiration);
    populateRolesAndPermissions(response, user);

    return response;
  }

  @Override
  @Transactional
  public DtoAuthResponse refreshToken(DtoRefreshToken dtoRefreshToken) {
    String refreshTokenString = dtoRefreshToken.getRefreshToken();

    // Refresh token'ı doğrula
    if (!jwtUtil.validateRefreshToken(refreshTokenString)) {
      throw new BadRequestException("Invalid or expired refresh token");
    }

    // Veritabanından refresh token'ı bul
    RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
        .orElseThrow(() -> new BadRequestException("Refresh token not found"));

    // Token iptal edilmiş mi kontrol et
    if (refreshToken.getIsRevoked()) {
      throw new BadRequestException("Refresh token has been revoked");
    }

    // Token süresi dolmuş mu kontrol et
    if (refreshToken.getExpiryDate().before(new Date())) {
      refreshTokenRepository.delete(refreshToken);
      throw new BadRequestException("Refresh token has expired");
    }

    // Kullanıcıyı bul
    User user = refreshToken.getUser();
    if (!user.getIsActive()) {
      throw new BadRequestException("User account is deactivated");
    }

    // Token version'ı artır (yeni token alındığında eski token'ları geçersiz kılmak
    // için)
    Long currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
    user.setTokenVersion(currentVersion + 1);
    user = userRepository.save(user);
    // Refresh token'da token version değişti, cache temizle
    userAuthCacheService.evictUserCache(user.getUsername());

    // Tenant ve loginSource bilgilerini mevcut refresh token'dan al
    String tenantId = jwtUtil.extractTenantId(refreshTokenString);
    String loginSource = jwtUtil.extractLoginSource(refreshTokenString);

    // Yeni access token oluştur (güncel version ile)
    String newToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTokenVersion(),
        tenantId, loginSource);

    // Eski refresh token'ı iptal et (Token Rotation için)
    refreshToken.setIsRevoked(true);
    refreshTokenRepository.save(refreshToken);

    // Yeni refresh token oluştur (Token Rotation)
    String newRefreshTokenString = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(),
        tenantId, loginSource);

    // Kullanıcının tüm eski refresh token'larını sil (unique constraint ihlalini
    // önlemek için)
    // Önce silme işlemini yap, sonra yeni token'ı kaydet
    refreshTokenRepository.deleteByUserId(user.getId());
    refreshTokenRepository.flush(); // Hemen commit et

    // Yeni refresh token'ı kaydet
    RefreshToken newRefreshToken = createRefreshTokenEntity(user, newRefreshTokenString);
    refreshTokenRepository.save(newRefreshToken);

    // Response oluştur
    DtoAuthResponse response = new DtoAuthResponse();
    response.setToken(newToken);
    response.setRefreshToken(newRefreshTokenString); // Yeni refresh token döndür
    response.setUserId(user.getId());
    response.setUsername(user.getUsername());
    response.setEmail(user.getEmail());
    response.setUserCode(userUtil.generateUserCode(user));
    response.setExpiredDate(System.currentTimeMillis() + accessTokenExpiration);
    populateRolesAndPermissions(response, user);

    return response;
  }

  // ════════════════════════════════════════════════════════════════════════
  // 2FA (TOTP) Metodları
  // ════════════════════════════════════════════════════════════════════════

  @Override
  @Transactional
  public DtoMfaSetupResponse initMfaSetup(Long userId, String tenantId) {
    User user = loadUserForMfa(userId, tenantId);

    // Her çağrıda yeni secret üret (henüz doğrulanmamış setup sıfırlanabilir)
    String plainSecret = totpService.generateSecret();
    String encryptedSecret = aesEncryptor.encrypt(plainSecret);

    user.setMfaSecret(encryptedSecret);
    user.setMfaEnabled(false);      // setup tamamlanana kadar aktif değil
    user.setMfaSetupVerified(false);
    userRepository.save(user);
    userAuthCacheService.evictUserCache(user.getUsername());

    String qrUri = totpService.buildQrUri(plainSecret, user.getUsername());
    return DtoMfaSetupResponse.builder()
        .secret(plainSecret)
        .qrUri(qrUri)
        .issuer("Elly CMS")
        .build();
  }

  @Override
  @Transactional
  public void completeMfaSetup(Long userId, DtoMfaSetupVerifyRequest request, String tenantId) {
    User user = loadUserForMfa(userId, tenantId);

    if (user.getMfaSecret() == null) {
      throw new BadRequestException("2FA kurulumu başlatılmamış. Önce /mfa/setup endpoint'ini çağırın.");
    }

    String plainSecret = aesEncryptor.decrypt(user.getMfaSecret());
    if (!totpService.verify(plainSecret, request.getCode())) {
      throw new UnauthorizedException("Geçersiz doğrulama kodu. Authenticator uygulamanızı kontrol edin.");
    }

    user.setMfaEnabled(true);
    user.setMfaSetupVerified(true);
    userRepository.save(user);
    userAuthCacheService.evictUserCache(user.getUsername());
  }

  @Override
  @Transactional
  public DtoAuthResponse verifyMfaLogin(DtoMfaVerifyRequest request) {
    // 1. MFA token'ı doğrula
    if (!jwtUtil.validateMfaToken(request.getMfaToken())) {
      throw new UnauthorizedException("Geçersiz veya süresi dolmuş MFA token. Lütfen yeniden giriş yapın.");
    }

    // 2. Token'dan bağlamı çıkar
    Long userId = jwtUtil.extractUserId(request.getMfaToken());
    String loginSource = jwtUtil.extractLoginSource(request.getMfaToken());
    String tenantId = jwtUtil.extractTenantId(request.getMfaToken());

    // 3. Kullanıcıyı yükle (doğru tenant context ile)
    User user = loadUserForMfa(userId, tenantId);

    if (!Boolean.TRUE.equals(user.getMfaEnabled()) || !Boolean.TRUE.equals(user.getMfaSetupVerified())) {
      throw new BadRequestException("Bu kullanıcı için 2FA etkin değil.");
    }

    // 4. TOTP kodunu doğrula
    String plainSecret = aesEncryptor.decrypt(user.getMfaSecret());
    if (!totpService.verify(plainSecret, request.getCode())) {
      throw new UnauthorizedException("Geçersiz 2FA kodu.");
    }

    // 5. Tam JWT üret
    return issueFullAuthToken(user, loginSource, tenantId);
  }

  @Override
  @Transactional
  public void disableMfa(Long userId, DtoMfaDisableRequest request, String tenantId) {
    User user = loadUserForMfa(userId, tenantId);

    if (!Boolean.TRUE.equals(user.getMfaEnabled())) {
      throw new BadRequestException("2FA zaten devre dışı.");
    }

    // Şifreyi doğrula
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
      throw new UnauthorizedException("Şifre hatalı.");
    }

    user.setMfaEnabled(false);
    user.setMfaSetupVerified(false);
    user.setMfaSecret(null);
    userRepository.save(user);
    userAuthCacheService.evictUserCache(user.getUsername());
  }

  /**
   * MFA işlemleri için kullanıcıyı doğru tenant context'te yükler.
   * Admin login = basedb, tenant login = tenantId.
   */
  private User loadUserForMfa(Long userId, String tenantId) {
    String originalTenant = TenantContext.getTenantId();
    // loginSource bilinmiyorsa tenantId'ye göre context belirle
    if (tenantId == null || tenantId.isBlank()) {
      TenantContext.setTenantId(null); // basedb fallback
    } else {
      TenantContext.setTenantId(tenantId);
    }
    try {
      return userRepository.findById(userId)
          .orElseThrow(() -> new UnauthorizedException("Kullanıcı bulunamadı."));
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }

  @Override
  public DtoTenantTokenResponse getPublicToken(String tenantId) {
    if (!tenantProperties.getDatasources().containsKey(tenantId)) {
      throw new BadRequestException("Unknown tenant: " + tenantId);
    }
    String token = jwtUtil.generateTenantToken(tenantId);
    return new DtoTenantTokenResponse(token, "Bearer", tenantId);
  }

  /**
   * User entity'den rolleri ve izinleri çıkararak DtoAuthResponse'a set eder.
   * User.roles FetchType.EAGER olduğu için ek DB sorgusu gerektirmez.
   */
  private void populateRolesAndPermissions(DtoAuthResponse response, User user) {
    if (user.getRoles() == null || user.getRoles().isEmpty()) {
      response.setRoles(List.of());
      response.setPermissions(List.of());
      return;
    }

    List<String> roles = user.getRoles().stream()
        .map(role -> role.getName())
        .sorted()
        .collect(Collectors.toList());

    List<String> permissions = user.getRoles().stream()
        .flatMap(role -> role.getPermissions() != null ? role.getPermissions().stream() : java.util.stream.Stream.empty())
        .map(permission -> permission.getName())
        .distinct()
        .sorted()
        .collect(Collectors.toList());

    response.setRoles(roles);
    response.setPermissions(permissions);
  }

  private RefreshToken createRefreshTokenEntity(User user, String tokenString) {
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken(tokenString);
    refreshToken.setUser(user);
    refreshToken.setIsRevoked(false);
    refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + refreshTokenExpiration));
    return refreshToken;
  }
}
