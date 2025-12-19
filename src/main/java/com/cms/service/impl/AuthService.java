package com.cms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import com.cms.dto.DtoAuthResponse;
import com.cms.dto.DtoLogin;
import com.cms.dto.DtoRefreshToken;
import com.cms.dto.DtoRegister;
import com.cms.entity.RefreshToken;
import com.cms.entity.User;
import com.cms.exception.BadRequestException;
import com.cms.exception.ConflictException;
import com.cms.exception.ResourceNotFoundException;
import com.cms.repository.RefreshTokenRepository;
import com.cms.repository.UserRepository;
import com.cms.service.IAuthService;
import com.cms.util.JwtUtil;

@Service
public class AuthService implements IAuthService {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtUtil jwtUtil;

  @Autowired
  private AuthenticationManager authenticationManager;

  @Autowired
  private RefreshTokenRepository refreshTokenRepository;

  @Value("${jwt.refresh.expiration:604800000}")
  private Long refreshTokenExpiration; // 7 gün (milisaniye cinsinden)

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
    user.setIsActive(true);

    User savedUser = userRepository.save(user);

    // JWT token oluştur
    String token = jwtUtil.generateToken(savedUser.getId(), savedUser.getUsername());

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

    // User bilgilerini al
    // CustomUserDetailsService her zaman username döndürür, bu yüzden direkt
    // username ile arama yapabiliriz
    UserDetails userDetails = (UserDetails) authentication.getPrincipal();
    String username = userDetails.getUsername();

    // Username ile user'ı bul (CustomUserDetailsService zaten username döndürüyor)
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

    // Kullanıcı aktif mi kontrol et
    if (!user.getIsActive()) {
      throw new BadRequestException("User account is deactivated");
    }

    // JWT token oluştur
    String token = jwtUtil.generateToken(user.getId(), user.getUsername());

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

    // Yeni access token oluştur
    String newToken = jwtUtil.generateToken(user.getId(), user.getUsername());

    // Response oluştur
    DtoAuthResponse response = new DtoAuthResponse();
    response.setToken(newToken);
    response.setRefreshToken(refreshTokenString); // Aynı refresh token'ı döndür
    response.setUserId(user.getId());
    response.setUsername(user.getUsername());
    response.setEmail(user.getEmail());

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
