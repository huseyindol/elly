package com.cms.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/** Kullanıcıya özel konular ve kullanıcı hedefli teslimat anahtarı tenant'a bağlıdır. */
class ChatTopicsTest {

  @Test
  void userTopicsCarryTheTenant() {
    assertEquals("/topic/tenant/t1/user/42/rtc", ChatTopics.userRtcTopic("t1", 42L));
    assertEquals("/topic/tenant/t1/user/42/membership", ChatTopics.userMembershipTopic("t1", 42L));
    assertEquals("/topic/tenant/t1/user/42/groups/joined", ChatTopics.userGroupJoinedTopic("t1", 42L));
    assertEquals("/topic/tenant/t1/user/42/groups/removed", ChatTopics.userGroupRemovedTopic("t1", 42L));
    // Aynı numara başka tenant'ta başka kullanıcı: konular ayrı.
    assertNotEquals(ChatTopics.userRtcTopic("t1", 42L), ChatTopics.userRtcTopic("t2", 42L));
  }

  @Test
  void userKeyIsTenantQualifiedAndNeverCollidesAcrossTenants() {
    assertEquals("t1:admin", ChatTopics.userKey("t1", "admin"));
    assertNotEquals(ChatTopics.userKey("t1", "admin"), ChatTopics.userKey("t2", "admin"));
    // Tenant ayracı ile benzer görünen adlar aynı anahtara düşmez ("t1" + ":a:b" karışması yok).
    assertNotEquals(ChatTopics.userKey("t1", "a:b"), ChatTopics.userKey("t1:a", "b"));
  }

  @Test
  void unknownTenantNeverMatchesASessionKey() {
    // Tenant bilinmiyorsa anahtar ":ad" olur; hiçbir oturum boş tenant ile kurulmadığından teslimat olmaz.
    // Bu, yanlış tenant'a teslimatten (tenant'lar arası sızıntı) güvenli taraftır.
    assertEquals(":admin", ChatTopics.userKey(null, "admin"));
    assertEquals(":admin", ChatTopics.userKey("", "admin"));
  }
}
