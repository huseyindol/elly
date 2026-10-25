package com.cms.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.cms.config.ChatWebSocketSecurityConfig;
import com.cms.config.DataSourceConfig;
import com.cms.config.JwtAuthenticationFilter;
import com.cms.config.UserAuthCacheService;
import com.cms.dto.CachedUserDetails;
import com.cms.dto.DtoAdminUserUpdate;
import com.cms.dto.DtoEmailLog;
import com.cms.dto.DtoMfaVerifyRequest;
import com.cms.entity.ChatGroup;
import com.cms.entity.ChatGroupType;
import com.cms.entity.EmailLog;
import com.cms.entity.Role;
import com.cms.entity.User;
import com.cms.enums.EmailStatus;
import com.cms.exception.BadRequestException;
import com.cms.exception.ForbiddenException;
import com.cms.exception.UnauthorizedException;
import com.cms.mapper.UserMapper;
import com.cms.repository.ChatGroupMemberRepository;
import com.cms.repository.ChatGroupRepository;
import com.cms.repository.EmailLogRepository;
import com.cms.repository.MailAccountRepository;
import com.cms.repository.RefreshTokenRepository;
import com.cms.repository.RoleRepository;
import com.cms.repository.UserRepository;
import com.cms.service.IAuthService;
import com.cms.service.IEmailService;
import com.cms.service.IMailAccountService;
import com.cms.service.INotificationService;
import com.cms.service.OAuthProviderClient;
import com.cms.service.TotpService;
import com.cms.util.AesEncryptor;
import com.cms.util.JwtUtil;
import com.cms.util.UserUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

class SecurityHardeningTest {

  private UserRepository userRepository;
  private ChatGroupRepository chatGroupRepository;
  private ChatGroupMemberRepository chatGroupMemberRepository;
  private UserAuthCacheService userAuthCacheService;
  private TotpService totpService;
  private AesEncryptor aesEncryptor;
  private JwtUtil jwtUtil;
  private AuthService authService;
  private UserService userService;
  private ChatWebSocketSecurityConfig wsSecurityConfig;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    chatGroupRepository = mock(ChatGroupRepository.class);
    chatGroupMemberRepository = mock(ChatGroupMemberRepository.class);
    userAuthCacheService = new UserAuthCacheService(null, new ObjectMapper());
    totpService = mock(TotpService.class);
    aesEncryptor = mock(AesEncryptor.class);
    jwtUtil = mock(JwtUtil.class);

    authService = new AuthService(
        userRepository,
        mock(PasswordEncoder.class),
        jwtUtil,
        mock(UserUtil.class),
        mock(AuthenticationManager.class),
        mock(RefreshTokenRepository.class),
        new DataSourceConfig.TenantDataSourceProperties(),
        userAuthCacheService,
        mock(RoleRepository.class),
        mock(MailAccountRepository.class),
        mock(IEmailService.class),
        mock(INotificationService.class),
        totpService,
        aesEncryptor,
        mock(OAuthProviderClient.class));

    userService = new UserService(
        userRepository,
        mock(RefreshTokenRepository.class),
        mock(RoleRepository.class),
        mock(PasswordEncoder.class),
        mock(UserMapper.class),
        userAuthCacheService,
        mock(IAuthService.class),
        new DataSourceConfig.TenantDataSourceProperties());

    wsSecurityConfig = new ChatWebSocketSecurityConfig(
        jwtUtil,
        userRepository,
        chatGroupRepository,
        chatGroupMemberRepository,
        new ObjectMapper(),
        null);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // ── A1: STOMP SUBSCRIBE Yetkilendirmesi ───────────────────────────────────

  @Test
  void stompSubscribe_presenceTopic_allowsAdminAndRejectsGuest() {
    Message<?> adminSub = buildSubscribeMessage("/topic/presence", "admin", "t1", 10L, "ROLE_ADMIN");
    StompHeaderAccessor adminAcc = StompHeaderAccessor.wrap(adminSub);
    assertDoesNotThrow(() -> wsSecurityConfig.authorizeSubscribe(adminAcc, adminSub));

    Message<?> guestSub = buildSubscribeMessage("/topic/presence", "website", "t1", null, "chat:guest");
    StompHeaderAccessor guestAcc = StompHeaderAccessor.wrap(guestSub);
    assertThrows(MessageDeliveryException.class,
        () -> wsSecurityConfig.authorizeSubscribe(guestAcc, guestSub));
  }

  @Test
  void stompSubscribe_userPersonalTopic_onlyAllowsMatchingUserId() {
    Message<?> ownSub = buildSubscribeMessage("/topic/user/42/rtc", "admin", "t1", 42L, "ROLE_EDITOR");
    assertDoesNotThrow(
        () -> wsSecurityConfig.authorizeSubscribe(StompHeaderAccessor.wrap(ownSub), ownSub));

    Message<?> otherSub = buildSubscribeMessage("/topic/user/99/rtc", "admin", "t1", 42L, "ROLE_EDITOR");
    assertThrows(MessageDeliveryException.class,
        () -> wsSecurityConfig.authorizeSubscribe(StompHeaderAccessor.wrap(otherSub), otherSub));
  }

  @Test
  void stompSubscribe_tenantGroup_enforcesTenantAndGroupAccess() {
    UUID groupId = UUID.randomUUID();
    ChatGroup privateDm = new ChatGroup();
    privateDm.setId(groupId);
    privateDm.setTenantId("t1");
    privateDm.setType(ChatGroupType.DM);
    privateDm.setVisibilityLevel(4);
    privateDm.setVisitorAccess(false);

    when(chatGroupRepository.findById(groupId)).thenReturn(Optional.of(privateDm));
    when(chatGroupMemberRepository.existsByIdGroupIdAndIdUserId(groupId, 10L)).thenReturn(true);
    when(chatGroupMemberRepository.existsByIdGroupIdAndIdUserId(groupId, 20L)).thenReturn(false);

    String topic = "/topic/tenant/t1/group/" + groupId;

    // Üye olan EDITOR abone olabilir
    Message<?> memberSub = buildSubscribeMessage(topic, "admin", "t1", 10L, "ROLE_EDITOR");
    assertDoesNotThrow(
        () -> wsSecurityConfig.authorizeSubscribe(StompHeaderAccessor.wrap(memberSub), memberSub));

    // Üye olmayan EDITOR özel DM'e abone olamaz
    Message<?> nonMemberSub = buildSubscribeMessage(topic, "admin", "t1", 20L, "ROLE_EDITOR");
    assertThrows(MessageDeliveryException.class,
        () -> wsSecurityConfig.authorizeSubscribe(StompHeaderAccessor.wrap(nonMemberSub), nonMemberSub));

    // Farklı tenant'taki guest abone olamaz
    Message<?> crossTenantGuest = buildSubscribeMessage(topic, "website", "t2", null, "chat:guest");
    assertThrows(MessageDeliveryException.class,
        () -> wsSecurityConfig.authorizeSubscribe(StompHeaderAccessor.wrap(crossTenantGuest), crossTenantGuest));
  }

  // ── A2 & A3: 2FA Brute-Force Sınırı ve initMfaSetup Koruması ─────────────

  @Test
  void initMfaSetup_rejectsWhenMfaAlreadyEnabledAndVerified() {
    User user = new User();
    ReflectionTestUtils.setField(user, "id", 5L);
    user.setUsername("admin");
    user.setMfaEnabled(true);
    user.setMfaSetupVerified(true);
    when(userRepository.findById(5L)).thenReturn(Optional.of(user));

    BadRequestException ex = assertThrows(
        BadRequestException.class,
        () -> authService.initMfaSetup(5L, "t1"));
    assertTrue(ex.getMessage().contains("2FA zaten etkin"));
  }

  @Test
  void verifyMfaLogin_locksOutAfterFiveFailedAttempts() {
    User user = new User();
    ReflectionTestUtils.setField(user, "id", 7L);
    user.setUsername("editor");
    user.setMfaEnabled(true);
    user.setMfaSetupVerified(true);
    user.setMfaSecret("enc-secret");

    when(jwtUtil.validateMfaToken("mfa-tok")).thenReturn(true);
    when(jwtUtil.extractUserId("mfa-tok")).thenReturn(7L);
    when(jwtUtil.extractLoginSource("mfa-tok")).thenReturn("admin");
    when(jwtUtil.extractTenantId("mfa-tok")).thenReturn("t1");
    when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    when(aesEncryptor.decrypt("enc-secret")).thenReturn("plain-secret");
    when(totpService.verify("plain-secret", "000000")).thenReturn(false);

    DtoMfaVerifyRequest req = new DtoMfaVerifyRequest();
    req.setMfaToken("mfa-tok");
    req.setCode("000000");

    for (int i = 1; i <= 4; i++) {
      UnauthorizedException ex = assertThrows(
          UnauthorizedException.class,
          () -> authService.verifyMfaLogin(req));
      assertEquals("Geçersiz 2FA kodu.", ex.getMessage());
    }

    // 5. hatalı denemede kilitlenir
    UnauthorizedException fifth = assertThrows(
        UnauthorizedException.class,
        () -> authService.verifyMfaLogin(req));
    assertTrue(fifth.getMessage().contains("Çok fazla hatalı 2FA denemesi"));

    // Kilitlendikten sonra doğru kod gönderilse bile reddedilir
    when(totpService.verify("plain-secret", "123456")).thenReturn(true);
    DtoMfaVerifyRequest validReq = new DtoMfaVerifyRequest();
    validReq.setMfaToken("mfa-tok");
    validReq.setCode("123456");
    assertThrows(UnauthorizedException.class, () -> authService.verifyMfaLogin(validReq));
  }

  // ── A4: Kullanıcı Düzenleme/Silme Rol Hiyerarşisi ─────────────────────────

  @Test
  void userService_adminCannotUpdateOrDeleteSuperAdminOrPeerAdmin() {
    setSecurityContextUser(10L, "admin1", Set.of("ROLE_ADMIN"));

    Role superAdminRole = new Role();
    superAdminRole.setName("SUPER_ADMIN");
    User targetSuper = new User();
    ReflectionTestUtils.setField(targetSuper, "id", 1L);
    targetSuper.setUsername("root");
    targetSuper.setRoles(Set.of(superAdminRole));

    Role adminRole = new Role();
    adminRole.setName("ADMIN");
    User targetPeerAdmin = new User();
    ReflectionTestUtils.setField(targetPeerAdmin, "id", 2L);
    targetPeerAdmin.setUsername("admin2");
    targetPeerAdmin.setRoles(Set.of(adminRole));

    Role editorRole = new Role();
    editorRole.setName("EDITOR");
    User targetEditor = new User();
    ReflectionTestUtils.setField(targetEditor, "id", 3L);
    targetEditor.setUsername("editor1");
    targetEditor.setRoles(Set.of(editorRole));

    when(userRepository.findById(1L)).thenReturn(Optional.of(targetSuper));
    when(userRepository.findById(2L)).thenReturn(Optional.of(targetPeerAdmin));
    when(userRepository.findById(3L)).thenReturn(Optional.of(targetEditor));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    DtoAdminUserUpdate updateReq = new DtoAdminUserUpdate();
    updateReq.setFirstName("Yeni");

    assertThrows(ForbiddenException.class, () -> userService.updateUser(1L, updateReq));
    assertThrows(ForbiddenException.class, () -> userService.updateUser(2L, updateReq));
    assertThrows(ForbiddenException.class, () -> userService.deleteUser(1L, "admin1"));
    assertThrows(ForbiddenException.class, () -> userService.setUserStatus(2L, false, "admin1"));

    // Kendinden düşük seviyedeki EDITOR'ü güncelleyebilir
    assertDoesNotThrow(() -> userService.updateUser(3L, updateReq));
  }

  // ── A5: DtoEmailLog Hata Detayı Alanı ─────────────────────────────────────

  @Test
  void emailService_listMapsErrorMessageIntoDtoEmailLog() {
    EmailLogRepository repo = mock(EmailLogRepository.class);
    EmailService emailService = new EmailService(
        repo,
        mock(RabbitTemplate.class),
        new ObjectMapper(),
        mock(IMailAccountService.class));

    EmailLog failedLog = new EmailLog();
    ReflectionTestUtils.setField(failedLog, "id", 99L);
    failedLog.setRecipient("user@example.com");
    failedLog.setSubject("Test");
    failedLog.setTemplateName("contact");
    failedLog.setStatus(EmailStatus.FAILED);
    failedLog.setRetryCount(3);
    failedLog.setErrorMessage("535-5.7.8 Username and Password not accepted");

    when(repo.findByStatus(EmailStatus.FAILED, PageRequest.of(0, 20)))
        .thenReturn(new PageImpl<>(List.of(failedLog)));

    Page<DtoEmailLog> result = emailService.list(EmailStatus.FAILED, PageRequest.of(0, 20));
    assertEquals(1, result.getContent().size());
    assertEquals("535-5.7.8 Username and Password not accepted",
        result.getContent().get(0).getErrorMessage());
  }

  // ── Yardımcılar ───────────────────────────────────────────────────────────

  private Message<?> buildSubscribeMessage(
      String destination, String loginSource, String tenantId, Long userId, String authority) {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
    accessor.setDestination(destination);
    Map<String, Object> attrs = new HashMap<>();
    attrs.put("loginSource", loginSource);
    attrs.put("tenantId", tenantId);
    if (userId != null) {
      attrs.put("userId", userId);
    }
    accessor.setSessionAttributes(attrs);
    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        "user", null, List.of(new SimpleGrantedAuthority(authority)));
    accessor.setUser(auth);
    accessor.setLeaveMutable(true);
    return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
  }

  private void setSecurityContextUser(Long userId, String username, Set<String> authorities) {
    CachedUserDetails cached = CachedUserDetails.builder()
        .id(userId)
        .username(username)
        .isActive(true)
        .authorities(authorities)
        .build();
    List<GrantedAuthority> granted = authorities.stream()
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
    JwtAuthenticationFilter.CachedUserPrincipal principal =
        new JwtAuthenticationFilter.CachedUserPrincipal(cached, granted);
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, null, granted));
  }
}
