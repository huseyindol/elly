package com.cms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Kullanıcı gruba davet edildiğinde veya gruptan çıkarıldığında
 * {@code /topic/user/{userId}/groups/joined|removed} topic'lerine giden payload.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DtoChatMembershipEvent {

  /** {@code JOINED} veya {@code REMOVED} */
  private String action;

  private UUID groupId;
  private Long userId;

  /** JOINED olayında dolu — sidebar'a eklemek için */
  private DtoChatGroup group;

  /** Panel banner metni (Türkçe, doğrudan gösterilebilir) */
  private String message;
}
