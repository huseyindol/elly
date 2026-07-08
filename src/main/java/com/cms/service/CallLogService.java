package com.cms.service;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.entity.CallLog;
import com.cms.entity.User;
import com.cms.enums.CallStatus;
import com.cms.repository.CallLogRepository;
import com.cms.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * WebRTC çağrılarının tenant-DB erişim noktası: call_logs yazımı + kullanıcı rol/isim
 * lookup'ları. Bu metotlar mevcut tenant DB'sine bağlanır.
 *
 * <p><b>Sözleşme:</b> Bu sınıf {@code TenantContext}'i SET ETMEZ. Çağıran ({@link CallSessionService})
 * her çağrıdan ÖNCE {@code TenantContext.setTenantId(call.tenantId)} yapmalıdır — @Transactional
 * sınırında bağlantı doğru tenant'a düşsün (self-invocation tuzağı olmadan, ayrı bean).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallLogService {

  private static final Set<String> PANEL_ROLES = Set.of("SUPER_ADMIN", "ADMIN", "EDITOR", "VIEWER");

  private final CallLogRepository callLogRepository;
  private final UserRepository userRepository;

  /** Bir kullanıcının çağrı için gereken bilgileri: username + rol seviyesi + TENANT mı. */
  public record UserCallInfo(Long userId, String username, int roleLevel, boolean isTenant) {
  }

  /** Ring-all hedefi: aktif panel kullanıcısının id + username'i (presence filtresi çağırana ait). */
  public record PanelUserRef(Long userId, String username) {
  }

  @Transactional(readOnly = true)
  public Optional<UserCallInfo> lookupUser(Long userId) {
    return userRepository.findById(userId).map(u -> {
      Set<String> roleNames = u.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet());
      boolean isTenant = roleNames.contains("TENANT");
      return new UserCallInfo(u.getId(), u.getUsername(), roleLevel(roleNames), isTenant);
    });
  }

  @Transactional(readOnly = true)
  public List<PanelUserRef> findPanelUsers() {
    return userRepository.findActiveUsersByRoleNames(PANEL_ROLES).stream()
        .map(u -> new PanelUserRef(u.getId(), u.getUsername()))
        .toList();
  }

  @Transactional
  public void createRinging(java.util.UUID callId, Long callerUserId, String callerDisplayName) {
    CallLog log = CallLog.builder()
        .id(callId)
        .callerUserId(callerUserId)
        .callerDisplayName(callerDisplayName)
        .status(CallStatus.RINGING)
        .startedAt(new Date())
        .build();
    callLogRepository.save(log);
  }

  @Transactional
  public void markAnswered(java.util.UUID callId, Long calleeUserId) {
    callLogRepository.findById(callId).ifPresent(cl -> {
      cl.setStatus(CallStatus.ANSWERED);
      cl.setCalleeUserId(calleeUserId);
      cl.setAnsweredAt(new Date());
      callLogRepository.save(cl);
    });
  }

  /** Terminal durum (COMPLETED/MISSED/REJECTED/CANCELLED) + endedAt. */
  @Transactional
  public void markTerminal(java.util.UUID callId, CallStatus status) {
    callLogRepository.findById(callId).ifPresent(cl -> {
      cl.setStatus(status);
      cl.setEndedAt(new Date());
      callLogRepository.save(cl);
    });
  }

  private static int roleLevel(Set<String> roleNames) {
    if (roleNames.contains("SUPER_ADMIN")) return 4;
    if (roleNames.contains("ADMIN")) return 3;
    if (roleNames.contains("EDITOR")) return 2;
    return 1; // VIEWER, TENANT vb.
  }
}
