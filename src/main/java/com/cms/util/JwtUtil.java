package com.cms.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

  @Value("${jwt.secret:mySecretKey123456789012345678901234567890}")
  private String secret;

  @Value("${jwt.encryption.secret:myEncryptionKey123456789012345678901234567890}")
  private String encryptionSecret;

  @Value("${jwt.expiration:86400000}")
  private Long expiration; // 24 saat (milisaniye cinsinden)

  @Value("${jwt.refresh.expiration:604800000}")
  private Long refreshExpiration; // 7 gün (milisaniye cinsinden)

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  private SecretKey getEncryptionKey() {
    // AES-256-GCM için 256-bit (32 byte) key gerekiyor
    // Eğer secret 32 byte'dan kısa ise, SHA-256 hash kullan
    byte[] keyBytes = encryptionSecret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      // Key'i 32 byte'a uzatmak için SHA-256 hash kullan
      try {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
        keyBytes = digest.digest(keyBytes);
      } catch (java.security.NoSuchAlgorithmException e) {
        throw new RuntimeException("SHA-256 algorithm not found", e);
      }
    } else if (keyBytes.length > 32) {
      // Key'i 32 byte'a kısalt
      byte[] truncated = new byte[32];
      System.arraycopy(keyBytes, 0, truncated, 0, 32);
      keyBytes = truncated;
    }
    // AES için javax.crypto.spec.SecretKeySpec kullanılmalı
    return new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Long extractUserId(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get("userId", Long.class);
  }

  public Long extractTokenVersion(String token) {
    Claims claims = extractAllClaims(token);
    return claims.get("tokenVersion", Long.class);
  }

  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    // JWE (encrypted) token'ı decrypt et
    return Jwts.parser()
        .decryptWith(getEncryptionKey())
        .build()
        .parseEncryptedClaims(token)
        .getPayload();
  }

  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  public String generateToken(Long userId, String username, Long tokenVersion) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("tokenVersion", tokenVersion);
    return createToken(claims, username);
  }

  private String createToken(Map<String, Object> claims, String subject) {
    // JWE (JSON Web Encryption) ile token oluştur
    // AES-256-GCM authenticated encryption kullanılıyor - hem gizlilik hem bütünlük
    // sağlar
    // signWith JJWT'de encryptWith ile birlikte kullanılamaz
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
        .compact();
  }

  public Boolean validateToken(String token, String username, Long currentTokenVersion) {
    try {
      final String extractedUsername = extractUsername(token);
      final Long tokenVersion = extractTokenVersion(token);
      return (extractedUsername.equals(username)
          && !isTokenExpired(token)
          && tokenVersion != null
          && tokenVersion.equals(currentTokenVersion));
    } catch (Exception e) {
      return false;
    }
  }

  public String generateRefreshToken(Long userId, String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("type", "refresh");
    return createRefreshToken(claims, username);
  }

  private String createRefreshToken(Map<String, Object> claims, String subject) {
    // JWE (JSON Web Encryption) ile refresh token oluştur
    // AES-256-GCM authenticated encryption kullanılıyor - hem gizlilik hem bütünlük
    // sağlar
    // signWith JJWT'de encryptWith ile birlikte kullanılamaz
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
        .encryptWith(getEncryptionKey(), Jwts.ENC.A256GCM)
        .compact();
  }

  public Boolean validateRefreshToken(String token) {
    try {
      Claims claims = extractAllClaims(token);
      String type = claims.get("type", String.class);
      return "refresh".equals(type) && !isTokenExpired(token);
    } catch (Exception e) {
      return false;
    }
  }
}
