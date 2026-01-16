package com.cms.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.CustomUserDetailsService;

import lombok.RequiredArgsConstructor;

import java.util.Date;

import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.entity.RefreshToken;
import com.cms.entity.User;
import com.cms.exception.BadRequestException;
import com.cms.exception.ConflictException;
import com.cms.repository.RefreshTokenRepository;
import com.cms.repository.UserRepository;
import com.cms.service.IAuthService;
import com.cms.util.JwtUtil;
import com.cms.util.UserUtil;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;
  private final UserUtil userUtil;
  private final AuthenticationManager authenticationManager;
  private final RefreshTokenRepository refreshTokenRepository;

  @Value("${jwt.refresh.expiration:3600000}")
  private Long refreshTokenExpiration;
  @Value("${jwt.expiration:1800000}")
  private Long accessTokenExpiration;

  @Override
  @Transactional
  public DtoAuthResponse register(DtoRegister dtoRegister) {
    // Email kontrolü
    if (userRepository.existsByEmail(dtoRegister.getEmail())) {
      throw new ConflictException("Email already exists: " + dtoRegister.getEmail());
    }

    // Username kontrolü
    if (userRepository.existsByUsername(dtoRegister.getUsername())) {
      throw new ConflictException("Username already exists: " + dtoRegister.getUsername());
    }

    // Yeni kullanıcı oluştur
    User user = new User();
    user.setUsername(dtoRegister.getUsername());
    user.setEmail(dtoRegister.getEmail());
    user.setPassword(passwordEncoder.encode(dtoRegister.getPassword()));
    user.setFirstName(dtoRegister.getFirstName());
    user.setLastName(dtoRegister.getLastName());
    user.setProvider("local"); // Local kayıt için
    user.setIsActive(true);

    User savedUser = userRepository.save(user);

    // Token version'ı artır (yeni token alındığında eski token'ları geçersiz kılmak
    // için)
    Long currentVersion = savedUser.getTokenVersion() != null ? savedUser.getTokenVersion() : 0L;
    savedUser.setTokenVersion(currentVersion + 1);
    savedUser = userRepository.save(savedUser);

    // JWT token oluştur (güncel version ile)
    String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getUsername(), savedUser.getTokenVersion());

    // Refresh token oluştur ve kaydet
    String refreshTokenString = jwtUtil.generateRefreshToken(savedUser.getId(), savedUser.getUsername());
    RefreshToken refreshToken = createRefreshTokenEntity(savedUser, refreshTokenString);
    refreshTokenRepository.save(refreshToken);

    // Response oluştur
    DtoAuthResponse response = new DtoAuthResponse();
    response.setToken(token);
    response.setRefreshToken(refreshTokenString);
    response.setUserId(savedUser.getId());
    response.setUsername(savedUser.getUsername());
    response.setEmail(savedUser.getEmail());
    response.setUserCode(userUtil.generateUserCode(savedUser));
    response.setExpiredDate(System.currentTimeMillis() + accessTokenExpiration);

    return response;
  }

  @Override
  @Transactional
  public DtoAuthResponse login(DtoLogin dtoLogin) {
    // Authentication işlemi
    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            dtoLogin.getUsernameOrEmail(),
            dtoLogin.getPassword()));

    // User bilgilerini authentication'dan al (tekrar sorgu yapmadan)
    CustomUserDetailsService.CustomUserPrincipal principal = (CustomUserDetailsService.CustomUserPrincipal) authentication
        .getPrincipal();
    User user = principal.getUser();

    // Kullanıcı aktif mi kontrol et (CustomUserPrincipal.isEnabled() ile de kontrol
    // ediliyor)
    if (!user.getIsActive()) {
      throw new BadRequestException("User account is deactivated");
    }

    // Token version'ı artır (yeni token alındığında eski token'ları geçersiz kılmak
    // için)
    Long currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
    user.setTokenVersion(currentVersion + 1);
    user = userRepository.save(user);

    // JWT token oluştur (güncel version ile)
    String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());

    // Refresh token oluştur
    String refreshTokenString = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

    // Mevcut refresh token'ı bul veya yeni oluştur
    RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId())
        .orElse(new RefreshToken());

    // Refresh token'ı güncelle veya yeni oluştur
    refreshToken.setToken(refreshTokenString);
    refreshToken.setUser(user);
    refreshToken.setIsRevoked(false);
    refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + refreshTokenExpiration));

    refreshTokenRepository.save(refreshToken);

    // Response oluştur
    DtoAuthResponse response = new DtoAuthResponse();
    response.setToken(token);
    response.setRefreshToken(refreshTokenString);
    response.setUserId(user.getId());
    response.setUsername(user.getUsername());
    response.setEmail(user.getEmail());
    response.setUserCode(userUtil.generateUserCode(user));
    response.setExpiredDate(System.currentTimeMillis() + accessTokenExpiration);

    return response;
  }

  @Override
  @Transactional
  public DtoAuthResponse refreshToken(DtoRefreshToken dtoRefreshToken) {
    String refreshTokenString = dtoRefreshToken.getRefreshToken();

    // Refresh token'ı doğrula
    if (!jwtUtil.validateRefreshToken(refreshTokenString)) {
      throw new BadRequestException("Invalid or expired refresh token");
    }

    // Veritabanından refresh token'ı bul
    RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
        .orElseThrow(() -> new BadRequestException("Refresh token not found"));

    // Token iptal edilmiş mi kontrol et
    if (refreshToken.getIsRevoked()) {
      throw new BadRequestException("Refresh token has been revoked");
    }

    // Token süresi dolmuş mu kontrol et
    if (refreshToken.getExpiryDate().before(new Date())) {
      refreshTokenRepository.delete(refreshToken);
      throw new BadRequestException("Refresh token has expired");
    }

    // Kullanıcıyı bul
    User user = refreshToken.getUser();
    if (!user.getIsActive()) {
      throw new BadRequestException("User account is deactivated");
    }

    // Token version'ı artır (yeni token alındığında eski token'ları geçersiz kılmak
    // için)
    Long currentVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0L;
    user.setTokenVersion(currentVersion + 1);
    user = userRepository.save(user);

    // Yeni access token oluştur (güncel version ile)
    String newToken = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getTokenVersion());

    // Eski refresh token'ı iptal et (Token Rotation için)
    refreshToken.setIsRevoked(true);
    refreshTokenRepository.save(refreshToken);

    // Yeni refresh token oluştur (Token Rotation)
    String newRefreshTokenString = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

    // Kullanıcının tüm eski refresh token'larını sil (unique constraint ihlalini
    // önlemek için)
    // Önce silme işlemini yap, sonra yeni token'ı kaydet
    refreshTokenRepository.deleteByUserId(user.getId());
    refreshTokenRepository.flush(); // Hemen commit et

    // Yeni refresh token'ı kaydet
    RefreshToken newRefreshToken = createRefreshTokenEntity(user, newRefreshTokenString);
    refreshTokenRepository.save(newRefreshToken);

    // Response oluştur
    DtoAuthResponse response = new DtoAuthResponse();
    response.setToken(newToken);
    response.setRefreshToken(newRefreshTokenString); // Yeni refresh token döndür
    response.setUserId(user.getId());
    response.setUsername(user.getUsername());
    response.setEmail(user.getEmail());
    response.setUserCode(userUtil.generateUserCode(user));
    response.setExpiredDate(System.currentTimeMillis() + accessTokenExpiration);

    return response;
  }

  private RefreshToken createRefreshTokenEntity(User user, String tokenString) {
    RefreshToken refreshToken = new RefreshToken();
    refreshToken.setToken(tokenString);
    refreshToken.setUser(user);
    refreshToken.setIsRevoked(false);
    refreshToken.setExpiryDate(new Date(System.currentTimeMillis() + refreshTokenExpiration));
    return refreshToken;
  }
}
