package com.cms.entity;

/**
 * ChatMessage sender'ının polymorphic tipi.
 * <ul>
 *   <li>{@link #ADMIN} → panel (admin) kullanıcısı.
 *       sender_id kendi tenant DB'sindeki users.id'yi gösterir; visitor_id ve session_id null.</li>
 *   <li>{@link #VISITOR} → Tenant Chat (TC) içinde <b>kayıtlı</b> tenant user'ı.
 *       visitor_id ilgili tenant DB visitor_identities.id'sini gösterir; diğerleri null.</li>
 *   <li>{@link #GUEST} → Tenant Chat (TC) içinde <b>anonim</b> website ziyaretçisi.
 *       session_id (guest token oturum kimliği) + sender_display_name dolu;
 *       sender_id ve visitor_id null.</li>
 * </ul>
 */
public enum ChatMessageSenderType {
  ADMIN,
  VISITOR,
  GUEST
}
