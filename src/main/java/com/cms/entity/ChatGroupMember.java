package com.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "chat_group_members")
@Getter
@Setter
@NoArgsConstructor
public class ChatGroupMember {

  @EmbeddedId
  private ChatGroupMemberId id;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length = 10)
  private ChatMemberRole role = ChatMemberRole.MEMBER;

  @Column(name = "joined_at", nullable = false)
  private Date joinedAt = new Date();
}
