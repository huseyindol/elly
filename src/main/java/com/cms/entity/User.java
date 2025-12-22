package com.cms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_username", columnList = "username", unique = true)
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = { "email" }, name = "uc_user_email"),
    @UniqueConstraint(columnNames = { "username" }, name = "uc_user_username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {
  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password")
  private String password; // OAuth kullanıcıları için null olabilir

  @Column(name = "provider")
  private String provider; // "local", "google", "facebook", "github", "x"

  @Column(name = "provider_id")
  private String providerId; // OAuth provider'dan gelen unique ID

  @Column(name = "first_name")
  private String firstName;

  @Column(name = "last_name")
  private String lastName;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "token_version", nullable = false)
  private Long tokenVersion = 0L; // Token versioning için - yeni token alındığında artırılır
}
