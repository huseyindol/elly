package com.cms.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cms.config.CustomUserDetailsService.CustomUserPrincipal;
import com.cms.config.DataSourceConfig;
import com.cms.config.TenantContext;
import com.cms.config.UserAuthCacheService;
import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoRefreshToken;
import com.cms.entity.RefreshToken;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.exception.BadRequestException;
import com.cms.repository.MailAccountRepository;
import com.cms.repository.RefreshTokenRepository;
import com.cms.repository.RoleRepository;
import com.cms.repository.UserRepository;
import com.cms.service.IEmailService;
import com.cms.service.INotificationService;
import com.cms.service.OAuthProviderClient;
import com.cms.service.TotpService;
import com.cms.util.AesEncryptor;
import com.cms.util.JwtUtil;
import com.cms.util.UserUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Giriş → yenileme el sıkışması: girişin veritabanına yazdığı değer, yenileme isteğinin aradığı
 * değerle aynı olmalı. Sertleştirme paketi (S5) arama tarafını SHA-256'ya çevirdi, yazma tarafını
 * unuttu — girişte verilen her yenileme anahtarı "Refresh token not found" ile reddediliyor, bütün
 * istemciler erişim anahtarının süresi dolunca (30 dk) yeniden girişe düşüyordu.
 */
class AuthServiceRefreshTokenTest {

  private static final String TENANT = "tenant1";

  /** refresh_tokens tablosunun kuklası; anahtar = tabloya yazılan token sütunu. */
  private final Map<String, RefreshToken> refreshTokensTable = new HashMap<>();

  private RefreshTokenRepository refreshTokenRepository;
  private JwtUtil jwtUtil;
  private UserRepository userRepository;
  private AuthenticationManager authenticationManager;
  private AuthService authService;
  private User user;

  @BeforeEach
  void setUp() {
    refreshTokenRepository = mock(RefreshTokenRepository.class);
    jwtUtil = mock(JwtUtil.class);
    userRepository = mock(UserRepository.class);
    authenticationManager = mock(AuthenticationManager.class);
    DataSourceConfig.TenantDataSourceProperties tenantProperties = new DataSourceConfig.TenantDataSourceProperties();
    tenantProperties.getDatasources().put(TENANT, new DataSourceConfig.TenantDataSourceConfig());

    authService = new AuthService(
        userRepository,
        mock(PasswordEncoder.class),
        jwtUtil,
        mock(UserUtil.class),
        authenticationManager,
        refreshTokenRepository,
        tenantProperties,
        new UserAuthCacheService(null, new ObjectMapper()),
        mock(RoleRepository.class),
        mock(MailAccountRepository.class),
        mock(IEmailService.class),
        mock(INotificationService.class),
        mock(TotpService.class),
        mock(AesEncryptor.class),
        mock(OAuthProviderClient.class));
    ReflectionTestUtils.setField(authService, "accessTokenExpiration", 1_800_000L);
    ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604_800_000L);

    // Panel kullanıcısı: şifresi doğrulanmış, aktif.
    Role viewer = new Role();
    viewer.setName("VIEWER");
    user = new User();
    ReflectionTestUtils.setField(user, "id", 21L); // kimlik sütunu: setter yok
    user.setUsername("mac-agent");
    user.setRoles(new HashSet<>(Set.of(viewer)));
    when(authenticationManager.authenticate(any())).thenReturn(
        new UsernamePasswordAuthenticationToken(new CustomUserPrincipal(user), null, List.of()));
    when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

    when(jwtUtil.generateToken(any(), any(), any(), any(), any()))
        .thenReturn("access-token-1", "access-token-2", "access-token-3");
    when(jwtUtil.generateRefreshToken(any(), any(), any(), any()))
        .thenReturn("refresh-token-1", "refresh-token-2", "refresh-token-3");
    when(jwtUtil.validateRefreshToken(anyString())).thenReturn(true);
    when(jwtUtil.extractTenantId(anyString())).thenReturn(TENANT);
    when(jwtUtil.extractLoginSource(anyString())).thenReturn("admin");

    // Tablo kuklası: girişin yazdığı satır, yenilemenin aradığı anahtarla birebir aranır.
    doAnswer(inv -> {
      RefreshToken row = new RefreshToken();
      row.setToken(inv.getArgument(1));
      row.setExpiryDate(inv.getArgument(2));
      row.setUser(user);
      refreshTokensTable.put(row.getToken(), row);
      return null;
    }).when(refreshTokenRepository).upsertRefreshToken(anyLong(), anyString(), any());
    when(refreshTokenRepository.findByToken(anyString()))
        .thenAnswer(inv -> Optional.ofNullable(refreshTokensTable.get(inv.getArgument(0))));
    when(refreshTokenRepository.save(any())).thenAnswer(inv -> {
      RefreshToken row = inv.getArgument(0);
      refreshTokensTable.put(row.getToken(), row);
      return row;
    });
    doAnswer(inv -> {
      refreshTokensTable.clear();
      return null;
    }).when(refreshTokenRepository).deleteByUserId(anyLong());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void refresh_acceptsTheTokenIssuedByLogin() {
    DtoAuthResponse login = authService.login(panelLogin(), "203.0.113.9");

    DtoAuthResponse refreshed = authService.refreshToken(refreshRequest(login.getRefreshToken()));

    assertEquals("access-token-2", refreshed.getToken());
    assertEquals("refresh-token-2", refreshed.getRefreshToken());
  }

  @Test
  void login_storesOnlyTheFingerprintOfTheRefreshToken() {
    String issued = authService.login(panelLogin(), "203.0.113.9").getRefreshToken();

    // S5: veritabanı sızsa bile yenileme anahtarı kullanılamaz — tabloda ham anahtar olmaz.
    assertTrue(refreshTokensTable.containsKey(sha256Hex(issued)));
    assertFalse(refreshTokensTable.containsKey(issued));
  }

  @Test
  void refresh_rotatesTheToken_oldOneIsGone_newOneWorks() {
    String first = authService.login(panelLogin(), "203.0.113.9").getRefreshToken();

    String second = authService.refreshToken(refreshRequest(first)).getRefreshToken();
    assertNotEquals(first, second);

    assertEquals("access-token-3", authService.refreshToken(refreshRequest(second)).getToken());
    assertThrows(BadRequestException.class, () -> authService.refreshToken(refreshRequest(first)));
  }

  private static DtoLogin panelLogin() {
    DtoLogin dto = new DtoLogin();
    dto.setUsernameOrEmail("mac-agent");
    dto.setPassword("parola");
    dto.setTenantId(TENANT);
    dto.setLoginType("admin");
    return dto;
  }

  private static DtoRefreshToken refreshRequest(String token) {
    DtoRefreshToken dto = new DtoRefreshToken();
    dto.setRefreshToken(token);
    return dto;
  }

  private static String sha256Hex(String value) throws RuntimeException {
    try {
      byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
