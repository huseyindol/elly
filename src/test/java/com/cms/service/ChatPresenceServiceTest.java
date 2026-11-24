package com.cms.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Çevrimiçi kaydın ömrü bağlantıda kurulur; açık oturumlar için zamanlayıcı tazeler, kapanmış oturumların
 * sahipleri için tazelemez (yoksa kapanan kullanıcı sonsuza dek çevrimiçi kalırdı).
 */
@SuppressWarnings("unchecked")
class ChatPresenceServiceTest {

  private RedisTemplate<String, Object> redis;
  private ValueOperations<String, Object> ops;
  private ChatPresenceService service;

  @BeforeEach
  void setUp() {
    redis = mock(RedisTemplate.class);
    ops = mock(ValueOperations.class);
    when(redis.opsForValue()).thenReturn(ops);
    service = new ChatPresenceService(redis);
    ReflectionTestUtils.setField(service, "presenceTtlSeconds", 300L);
  }

  @Test
  void openSessionsAreRefreshedAndClosedOnesAreNot() {
    when(ops.increment("chat:presence:tenant1:1")).thenReturn(1L);
    when(ops.increment("chat:presence:tenant1:2")).thenReturn(1L);
    when(redis.hasKey("chat:presence:tenant1:1")).thenReturn(true);
    when(redis.hasKey("chat:presence:tenant1:2")).thenReturn(true);
    service.userConnected("tenant1", 1L, "s1");
    service.userConnected("tenant1", 1L, "s2");
    service.userConnected("tenant1", 2L, "s3");

    when(ops.decrement("chat:presence:tenant1:2")).thenReturn(0L);
    service.userDisconnected("tenant1", 2L, "s3");

    clearInvocations(redis, ops);
    service.refreshLiveSessions();

    // Aynı kullanıcının iki oturumu olsa da kayıt bir kez tazelenir; 2'nin oturumu kapandı.
    verify(redis).hasKey("chat:presence:tenant1:1");
    verify(redis).expire("chat:presence:tenant1:1", 300L, TimeUnit.SECONDS);
    verifyNoMoreInteractions(redis); // başka hiçbir kullanıcı için kayda dokunulmaz
    verifyNoInteractions(ops);        // ve hiçbir şey yazılmaz
  }

  @Test
  void sameUserNumberInAnotherTenantIsAnotherRecord() {
    // Kullanıcı numaraları tenant veritabanına özeldir: tenant2'deki 2 çevrimiçi olsa da tenant1'deki 2 değil.
    when(ops.increment("chat:presence:tenant2:2")).thenReturn(1L);
    when(redis.hasKey("chat:presence:tenant2:2")).thenReturn(true);
    service.userConnected("tenant2", 2L, "s-other");
    when(ops.get("chat:presence:tenant2:2")).thenReturn(1L);
    when(ops.get("chat:presence:tenant1:2")).thenReturn(null);

    assertTrue(service.isOnline("tenant2", 2L));
    assertFalse(service.isOnline("tenant1", 2L));

    service.userDisconnected("tenant2", 2L, "s-other");
    verify(redis, never()).delete("chat:presence:tenant1:2");
  }

  @Test
  void expiredRecordIsNotRecreatedByTheRefresh() {
    when(ops.increment("chat:presence:tenant1:7")).thenReturn(1L);
    when(redis.hasKey("chat:presence:tenant1:7")).thenReturn(false); // süre dolmuş / başka instance sildi
    service.userConnected("tenant1", 7L, "s7");

    clearInvocations(redis, ops);
    service.refreshLiveSessions();

    verify(redis, never()).expire("chat:presence:tenant1:7", 300L, TimeUnit.SECONDS);
    verifyNoInteractions(ops); // hiçbir yazma yok: kayıt yeniden doğmaz
  }

  @Test
  void oneFailingUserDoesNotStopTheOthers() {
    when(ops.increment("chat:presence:tenant1:1")).thenReturn(1L);
    when(ops.increment("chat:presence:tenant1:2")).thenReturn(1L);
    service.userConnected("tenant1", 1L, "s1");
    service.userConnected("tenant1", 2L, "s2");
    when(redis.hasKey("chat:presence:tenant1:1")).thenThrow(new IllegalStateException("redis down"));
    when(redis.hasKey("chat:presence:tenant1:2")).thenReturn(true);

    clearInvocations(redis, ops); // yalnız tazeleme turunun çağrıları sayılır
    service.refreshLiveSessions();

    verify(redis, times(1)).expire("chat:presence:tenant1:2", 300L, TimeUnit.SECONDS);
  }

  @Test
  void connectAndDisconnectCountingIsUnchanged() {
    when(ops.increment("chat:presence:tenant1:1")).thenReturn(1L, 2L);
    when(ops.decrement("chat:presence:tenant1:1")).thenReturn(1L, 0L);

    assertTrue(service.userConnected("tenant1", 1L, "s1"));   // ilk oturum → ONLINE
    assertFalse(service.userConnected("tenant1", 1L, "s2"));  // ikinci oturum → duyuru yok
    assertFalse(service.userDisconnected("tenant1", 1L, "s1"));
    assertTrue(service.userDisconnected("tenant1", 1L, "s2")); // son oturum → OFFLINE

    verify(redis, times(1)).delete("chat:presence:tenant1:1");
  }

  @Test
  void aRedisFailureOnConnectStillLeavesNoSessionToRefresh() {
    when(ops.increment("chat:presence:tenant1:1")).thenThrow(new IllegalStateException("redis down"));

    try {
      service.userConnected("tenant1", 1L, "s1");
    } catch (IllegalStateException expected) {
      // dinleyiciye yayılır; burada ilgilendiğimiz şey oturum listesi
    }

    clearInvocations(redis, ops);
    service.refreshLiveSessions();

    verifyNoInteractions(redis, ops);
  }

  @Test
  void sessionWithoutIdIsCountedButNeverTracked() {
    when(ops.increment("chat:presence:tenant1:1")).thenReturn(1L);
    service.userConnected("tenant1", 1L, null);

    clearInvocations(redis, ops);
    service.refreshLiveSessions();

    verifyNoInteractions(redis, ops);
  }
}
