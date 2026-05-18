package com.cms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Entity
@Table(name = "chat_message_reads")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessageRead {

  @EmbeddedId
  private ChatMessageReadId id;

  @Column(name = "read_at", nullable = false)
  private Date readAt = new Date();
}
