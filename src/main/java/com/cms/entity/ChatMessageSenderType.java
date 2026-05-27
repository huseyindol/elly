package com.cms.entity;

/**
 * ChatMessage sender'ının polymorphic tipi.
 * <ul>
 *   <li>{@link #ADMIN} → mevcut admin chat (AC) ve TC içinde admin kullanıcı.
 *       sender_id basedb users.id'sini gösterir; visitor_id null.</li>
 *   <li>{@link #VISITOR} → Tenant Chat (TC) içinde website ziyaretçisi.
 *       visitor_id ilgili tenant DB visitor_identities.id'sini gösterir; sender_id null.</li>
 * </ul>
 */
public enum ChatMessageSenderType {
  ADMIN,
  VISITOR
}
