package com.cms.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.cms.config.CustomUserDetailsService;
import com.cms.config.DataSourceConfig;
import com.cms.config.UserAuthCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoMfaDisableRequest;
import com.cms.dto.DtoMfaSetupResponse;
import com.cms.dto.DtoMfaSetupVerifyRequest;
import com.cms.dto.DtoMfaStatusResponse;
import com.cms.dto.DtoMfaVerifyRequest;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.dto.DtoSocialLogin;
import com.cms.dto.DtoGuestTokenRequest;
import com.cms.dto.DtoGuestTokenResponse;
import com.cms.dto.EmailRequest;
import com.cms.service.OAuthProviderClient;
import com.cms.service.OAuthProviderClient.SocialProfile;
import com.cms.service.TotpService;
import com.cms.util.AesEncryptor;
import com.cms.entity.RefreshToken;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.exception.BadRequestException;
import com.cms.exception.ConflictException;
import com.cms.exception.ForbiddenException;
import com.cms.exception.TenantRequiredException;
import com.cms.exception.UnauthorizedException;
import com.cms.enums.NotificationType;
import com.cms.repository.MailAccountRepository;
import com.cms.repository.RefreshTokenRepository;
import com.cms.repository.RoleRepository;
import com.cms.repository.UserRepository;
import com.cms.service.IAuthService;
import com.cms.service.IEmailService;
import com.cms.service.INotificationService;
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
  private final INotificationService notificationService;
  private final TotpService totpService;
  private final AesEncryptor aesEncryptor;
  private final OAuthProviderClient oAuthProviderClient;

  @Value("${jwt.refresh.expiration:3600000}")
  private Long refreshTokenExpiration;
  @Value("${jwt.expiration:1800000}")
  private Long accessTokenExpiration;
  @Value("${app.frontend-url.tenant:http://localhost:3000}")
  private String tenantFrontendUrl;
  @Value("${app.frontend-url.panel:http://localhost:3001}")
  private String panelFrontendUrl;

  @Override
  @Transactional
  public DtoAuthResponse register(DtoRegister dtoRegister) {
    // Tenant context AuthController.register tarafından @Transactional ÖNCESİ kuruluyor
    // (body'deki tenantId'den). Burada tek görev: context'in gerçekten kurulduğunu doğrulamak.
    // null/blank → TENANT_REQUIRED (basedb / sessiz fallback yok). Tenant id ayrıca config'te
    // tanımlı bir tenant'a karşılık gelmeli — gelmiyorsa BadRequest.
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new TenantRequiredException("Register icin tenantId zorunludur");
    }
    if (!tenantProperties.getDatasources().containsKey(tenantId)) {
      throw new BadRequestException("Tenant not found: " + tenantId);
    }

    // Email ve username kontrolü
    if (userRepository.existsByEmail(dtoRegister.getEmail())) {
      throw new ConflictException("Email already exists: " + dtoRegister.getEmail());
    }
    if (userRepository.existsByUsername(dtoRegister.getUsername())) {
      throw new ConflictException("Username already exists: " + dtoRegister.getUsername());
    }

    // Tenant register → TENANT rolü (tenant-site kullanıcısı, panel'e GİREMEZ). TENANT rolü
    // yoksa (seed eksik) graceful fallback: kullanıcı rolsüz kalır; admin sonradan rol atar.
    Role tenantRole = roleRepository.findByName("TENANT").orElse(null);

    String verificationToken = UUID.randomUUID().toString();

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

    if (tenantRole != null) {
      user.getRoles().add(tenantRole);
    }

    User savedUser = userRepository.save(user);

    triggerUserRegisteredNotification(savedUser, tenantId);

    // Doğrulama e-postası gönder (hata olursa kayıt yine tamamlanır)
    // Public register (tenant kullanıcı) → tenant'a özgü frontend URL'i. Her tenant
    // kendi domain'inde register olduğu için per-tenant config'ten alınır; yoksa global
    // fallback'e (tenantFrontendUrl) düşülür.
    String tenantUrl = tenantProperties.getDatasources().get(tenantId).getFrontendUrl();
    if (tenantUrl == null || tenantUrl.isBlank()) {
      tenantUrl = tenantFrontendUrl;
    }
    sendVerificationEmail(savedUser, tenantId, verificationToken, tenantUrl);

    DtoAuthResponse response = new DtoAuthResponse();
    response.setUserId(savedUser.getId());
    response.setUsername(savedUser.getUsername());
    response.setEmail(savedUser.getEmail());
    response.setMessage("Kayıt başarılı. Lütfen e-posta adresinize gönderilen doğrulama bağlantısına tıklayın.");

    return response;
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

  private void triggerUserRegisteredNotification(User user, String tenantId) {
    try {
      Map<String, Object> metadata = new java.util.HashMap<>();
      metadata.put("userId", user.getId());
      metadata.put("username", user.getUsername());
      notificationService.notifySuperAdminAndAdminUsers(
          NotificationType.USER_REGISTERED,
          "Yeni kullanici kaydi",
          user.getUsername() + " (" + user.getEmail() + ") kayit oldu",
          "/users",
          tenantId,
          metadata);
    } catch (Exception ex) {
      log.error("USER_REGISTERED bildirimi tetiklenemedi: userId={}, hata={}",
          user.getId(), ex.getMessage(), ex);
    }
  }

  /**
   * Mail göndermenin tek noktası. {@code frontendUrl} caller tarafından geçilir:
   * tenant register → tenant frontend; admin create → panel frontend. Audience hint
   * body'de değil — endpoint tipinden çıkar (decoupled, spoof'lanamaz).
   */
  @Override
  public void sendVerificationEmail(User user, String tenantId, String verificationToken,
      String frontendUrl) {
    // Mail hesapları artık her tenant'ın KENDİ DB'sinde (basedb yok). Context'i o tenant'a al.
    String currentTenant = TenantContext.getTenantId();
    try {
      if (tenantId != null && !tenantId.isBlank()) {
        TenantContext.setTenantId(tenantId);
      }

      var mailAccount = (tenantId != null && !tenantId.isBlank())
          ? mailAccountRepository.findByTenantIdAndIsPrimaryTrueAndActiveTrue(tenantId)
          : java.util.Optional.<com.cms.entity.MailAccount>empty();

      if (mailAccount.isEmpty()) {
        log.warn("Doğrulama e-postası gönderilemedi — '{}' tenant'ında aktif primary mail hesabı yok", tenantId);
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

    // Login (admin VEYA tenant) tenant DB'sinde doğrulanır (basedb yok).
    // Panel admin: tenantId body'de gelir. Tenant site: /api/v1/public/{tid}/auth/login →
    // PublicApiFilter context'i URL'den set eder (body'de tenantId YOK).
    if (tenantId != null && !tenantId.isBlank()) {
      TenantContext.setTenantId(tenantId);
    }
    String effectiveTenantId = TenantContext.getTenantId();
    if (effectiveTenantId == null || effectiveTenantId.isBlank()) {
      throw new BadRequestException("Login için tenant gerekli (tenantId veya /api/v1/public/{tid})");
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

    // Panel ↔ tenant ayrımı (simetrik). Tam izolasyon: cross-tenant kavramı YOK.
    //   • Panel rolleri: SUPER_ADMIN, ADMIN, EDITOR, VIEWER → panel'e girer
    //   • Tenant rolü:  TENANT                              → tenant-site'a girer
    // Login filtresi simetrik: iki dünya birbirine geçemez (panel kullanıcısı tenant site'a,
    // tenant kullanıcısı panel'e giremez).
    Set<String> panelRoleNames = Set.of("SUPER_ADMIN", "ADMIN", "EDITOR", "VIEWER");
    boolean isPanelUser = user.getRoles() != null && user.getRoles().stream()
        .anyMatch(r -> panelRoleNames.contains(r.getName()));
    boolean isTenantUser = user.getRoles() != null && user.getRoles().stream()
        .anyMatch(r -> "TENANT".equals(r.getName()));
    if (isAdminLogin && !isPanelUser) {
      throw new ForbiddenException("Bu kullanıcı panel girişi için yetkili değil");
    }
    if (!isAdminLogin && !isTenantUser) {
      throw new ForbiddenException("Panel kullanıcıları yalnız panel'den giriş yapabilir");
    }

    String loginSource = isAdminLogin ? "admin" : "tenant";

    // ── 2FA kontrolü ────────────────────────────────────────────────────
    // Şifre doğrulandı; MFA aktifse kısa ömürlü mfaToken dön, JWT henüz yok.
    // tokenVersion bu adımda artırılmaz — MFA doğrulandığında artırılır.
    if (Boolean.TRUE.equals(user.getMfaEnabled()) && Boolean.TRUE.equals(user.getMfaSetupVerified())) {
      String mfaToken = jwtUtil.generateMfaToken(user.getId(), loginSource, effectiveTenantId);
      DtoAuthResponse mfaResponse = new DtoAuthResponse();
      mfaResponse.setMfaRequired(true);
      mfaResponse.setMfaToken(mfaToken);
      return mfaResponse;
    }

    return issueFullAuthToken(user, loginSource, effectiveTenantId);
  }

  @Override
  @Transactional
  public DtoAuthResponse socialLogin(String provider, DtoSocialLogin dto) {
    // Tenant context AuthController tarafından @Transactional ÖNCESİ kurulur
    // (public URL veya body tenantId'si). Burada yalnız doğrula.
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new TenantRequiredException("Sosyal giriş için tenant gerekli (/api/v1/public/{tid})");
    }
    if (!tenantProperties.getDatasources().containsKey(tenantId)) {
      throw new BadRequestException("Tenant not found: " + tenantId);
    }

    // code → provider takası + doğrulanmış profil (dış HTTP; hata durumları anlaşılır mesajlarla)
    SocialProfile profile = oAuthProviderClient.fetchProfile(provider, dto.getCode(), dto.getRedirectUri());

    // 1) provider+providerId ile mevcut hesap
    User user = userRepository
        .findByProviderAndProviderId(profile.provider(), profile.providerId())
        .orElse(null);

    if (user == null) {
      // 2) E-posta eşleşmesi → OTOMATİK BAĞLAMA (provider e-postayı doğrulamış kabul edilir).
      //    Panel rollü hesaplara sosyal girişle DOKUNULMAZ — panel/tenant simetrisi korunur.
      User byEmail = userRepository.findByEmail(profile.email()).orElse(null);
      if (byEmail != null) {
        if (isPanelUser(byEmail)) {
          throw new ForbiddenException("Panel hesapları sosyal girişle kullanılamaz");
        }
        byEmail.setProvider(profile.provider());
        byEmail.setProviderId(profile.providerId());
        byEmail.setEmailVerified(true);
        user = userRepository.save(byEmail);
        log.info("Sosyal hesap bağlandı: {} → {} ({})", profile.provider(), user.getUsername(), tenantId);
      } else {
        // 3) Yeni kayıt: TENANT rolü, şifresiz (password NULL), e-posta doğrulanmış.
        Role tenantRole = roleRepository.findByName("TENANT").orElse(null);
        User newUser = new User();
        newUser.setUsername(uniqueUsername(profile.usernameHint()));
        newUser.setEmail(profile.email());
        newUser.setPassword(null);
        newUser.setFirstName(profile.firstName());
        newUser.setLastName(profile.lastName());
        newUser.setProvider(profile.provider());
        newUser.setProviderId(profile.providerId());
        newUser.setIsActive(true);
        newUser.setEmailVerified(true); // provider doğruladı → verification maili atlanır
        if (tenantRole != null) {
          newUser.getRoles().add(tenantRole);
        }
        user = userRepository.save(newUser);
        triggerUserRegisteredNotification(user, tenantId);
        log.info("Sosyal kayıt: {} kullanıcı={} ({})", profile.provider(), user.getUsername(), tenantId);
      }
    }

    if (!user.getIsActive()) {
      throw new BadRequestException("User account is deactivated");
    }
    // provider+providerId ile bulunan hesap panel rollüyse de reddet (tutarlılık)
    if (isPanelUser(user)) {
      throw new ForbiddenException("Panel hesapları sosyal girişle kullanılamaz");
    }

    // MFA: normal login ile aynı davranış — aktifse önce mfaToken.
    if (Boolean.TRUE.equals(user.getMfaEnabled()) && Boolean.TRUE.equals(user.getMfaSetupVerified())) {
      String mfaToken = jwtUtil.generateMfaToken(user.getId(), "tenant", tenantId);
      DtoAuthResponse mfaResponse = new DtoAuthResponse();
      mfaResponse.setMfaRequired(true);
      mfaResponse.setMfaToken(mfaToken);
      return mfaResponse;
    }

    return issueFullAuthToken(user, "tenant", tenantId);
  }

  /** Panel rolü (SUPER_ADMIN/ADMIN/EDITOR/VIEWER) taşıyor mu — sosyal giriş bu hesaplara kapalı. */
  private boolean isPanelUser(User user) {
    Set<String> panelRoleNames = Set.of("SUPER_ADMIN", "ADMIN", "EDITOR", "VIEWER");
    return user.getRoles() != null && user.getRoles().stream()
        .anyMatch(r -> panelRoleNames.contains(r.getName()));
  }

  /** E-posta local-part'ından benzersiz username üretir (çakışmada sayısal ek). */
  private String uniqueUsername(String hint) {
    String base = (hint == null || hint.isBlank()) ? "user" : hint.replaceAll("[^a-zA-Z0-9._-]", "");
    if (base.isBlank()) {
      base = "user";
    }
    String candidate = base;
    int suffix = 1;
    while (userRepository.existsByUsername(candidate)) {
      candidate = base + suffix++;
      if (suffix > 500) { // pratik güvenlik sınırı
        candidate = base + UUID.randomUUID().toString().substring(0, 8);
        break;
      }
    }
    return candidate;
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
    long now = System.currentTimeMillis();
    response.setExpiredDate(now + accessTokenExpiration);
    response.setRefreshExpiredDate(now + refreshTokenExpiration);
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

    // Tenant'ı refresh token'ın tenantId claim'inden çöz ve context'i set et.
    // Refresh isteğinde access token header'i yok/expired olabilir → refresh token VE user
    // DOĞRU tenant DB'sinde aranmalı (basedb fallback yok). Eski logout bug'ının kökü buydu.
    String refreshTenantId = jwtUtil.extractTenantId(refreshTokenString);
    if (refreshTenantId == null || refreshTenantId.isBlank()) {
      throw new BadRequestException("Refresh token tenant bilgisi icermiyor");
    }
    TenantContext.setTenantId(refreshTenantId);

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
    long now = System.currentTimeMillis();
    response.setExpiredDate(now + accessTokenExpiration);
    response.setRefreshExpiredDate(now + refreshTokenExpiration);
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

    if (Boolean.TRUE.equals(user.getMfaEnabled()) && Boolean.TRUE.equals(user.getMfaSetupVerified())) {
      throw new BadRequestException(
          "2FA zaten etkin. Yeniden kurmak için önce mevcut 2FA'yı devre dışı bırakın.");
    }

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

    // 2b. Brute-force koruması: 5 dakikalık pencerede en fazla 5 hatalı TOTP denemesi
    if (userAuthCacheService.isMfaLockedOut(tenantId, userId)) {
      throw new UnauthorizedException(
          "Çok fazla hatalı 2FA denemesi yapıldı. Lütfen yeniden giriş yapın.");
    }

    // 3. Kullanıcıyı yükle (doğru tenant context ile)
    User user = loadUserForMfa(userId, tenantId);

    if (!Boolean.TRUE.equals(user.getMfaEnabled()) || !Boolean.TRUE.equals(user.getMfaSetupVerified())) {
      throw new BadRequestException("Bu kullanıcı için 2FA etkin değil.");
    }

    // 4. TOTP kodunu doğrula
    String plainSecret = aesEncryptor.decrypt(user.getMfaSecret());
    if (!totpService.verify(plainSecret, request.getCode())) {
      int attempts = userAuthCacheService.recordFailedMfaAttempt(tenantId, userId);
      if (attempts >= UserAuthCacheService.MAX_MFA_VERIFY_ATTEMPTS) {
        throw new UnauthorizedException(
            "Çok fazla hatalı 2FA denemesi yapıldı. Lütfen yeniden giriş yapın.");
      }
      throw new UnauthorizedException("Geçersiz 2FA kodu.");
    }

    userAuthCacheService.clearMfaAttempts(tenantId, userId);

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

  @Override
  @Transactional(readOnly = true)
  public DtoMfaStatusResponse getMfaStatus(Long userId, String tenantId) {
    User user = loadUserForMfa(userId, tenantId);
    return DtoMfaStatusResponse.builder()
        .mfaEnabled(Boolean.TRUE.equals(user.getMfaEnabled()))
        .build();
  }

  /**
   * MFA işlemleri için kullanıcıyı doğru tenant context'te yükler (tenantId = kullanıcının
   * tenant'ı; basedb yok).
   */
  private User loadUserForMfa(Long userId, String tenantId) {
    String originalTenant = TenantContext.getTenantId();
    // tenantId verilmişse o tenant context'inde; verilmemişse MEVCUT context korunur.
    if (tenantId != null && !tenantId.isBlank()) {
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
  public DtoGuestTokenResponse getGuestToken(DtoGuestTokenRequest request) {
    // tenantId body'den; yoksa public path'ten gelen TenantContext'ten alınır
    // (/api/v1/public/{tenantId}/auth/guest-token → PublicApiFilter TenantContext'i set eder).
    String tenantId = (request.getTenantId() != null && !request.getTenantId().isBlank())
        ? request.getTenantId()
        : TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new BadRequestException("tenantId zorunludur");
    }
    if (!tenantProperties.getDatasources().containsKey(tenantId)) {
      throw new BadRequestException("Unknown tenant: " + tenantId);
    }
    // displayName public endpoint'ten gelen kullanıcı girdisi — HTML/script temizlenir
    // (stored XSS önlemi). Mesaj içeriğiyle aynı strateji: Jsoup Safelist.none().
    // Entity-encode sonrası uzunluk artabileceği için DB kolonuna (VARCHAR 100) göre kırpılır.
    String displayName = Jsoup.clean(request.getDisplayName(), Safelist.none()).trim();
    if (displayName.length() > 100) {
      displayName = displayName.substring(0, 100);
    }
    if (displayName.isBlank()) {
      displayName = "guest";
    }
    // sessionId: client'in kalici id'si (localStorage) gecerli UUID ise kullanilir, degilse uretilir.
    // Ayni tarayicidan donen guest ayni id'yi gonderir -> eski mesajlari "kendi" olarak eslesir.
    String sessionId = normalizeClientId(request.getClientId());
    String token = jwtUtil.generateGuestToken(displayName, sessionId, tenantId);
    return DtoGuestTokenResponse.builder()
        .token(token)
        .expiresIn(jwtUtil.getGuestExpirationSeconds())
        .displayName(displayName)
        .tenantId(tenantId)
        .sessionId(sessionId)
        .build();
  }

  /** Client'in gonderdigi kalici id'yi dogrular: gecerli UUID ise canonical formda dondurur, degilse yeni uretir. */
  private String normalizeClientId(String clientId) {
    if (clientId != null && !clientId.isBlank()) {
      try {
        return UUID.fromString(clientId.trim()).toString();
      } catch (IllegalArgumentException ignored) {
        // gecersiz format -> yeni uret
      }
    }
    return UUID.randomUUID().toString();
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
