package com.cms.dto;

import com.cms.entity.ChatMemberRole;
import lombok.Data;

import java.util.Date;

@Data
public class DtoChatMember {
  private Long userId;
  private String username;
  private String firstName;
  private String lastName;
  private ChatMemberRole role;
  private Date joinedAt;
}
