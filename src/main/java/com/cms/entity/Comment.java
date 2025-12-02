package com.cms.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_post_id", columnList = "post_id"),
    @Index(name = "idx_comment_parent_id", columnList = "parent_comment_id"),
    @Index(name = "idx_comment_status", columnList = "status"),
    @Index(name = "idx_comment_post_status", columnList = "post_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment extends BaseEntity {
  private String name;
  @Column(name = "email", nullable = false, length = 255)
  private String email;
  private String content;
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", referencedColumnName = "id")
  private Post post;
  // Cascade burada OLMAMALI - child silinince parent silinmemeli
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_comment_id", referencedColumnName = "id")
  private Comment parentComment;

  // Cascade burada OLMALI - parent silinince children silinmeli
  @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  private List<Comment> subComments = new ArrayList<>();

  private Boolean status;
}
