package com.cms.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.dto.DtoChangePassword;
import com.cms.dto.DtoUserResponse;
import com.cms.dto.DtoUserUpdate;
import com.cms.entity.User;
import com.cms.exception.BadRequestException;
import com.cms.exception.ConflictException;
import com.cms.mapper.UserMapper;
import com.cms.repository.UserRepository;
import com.cms.service.IUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService implements IUserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;

  @Override
  public DtoUserResponse getMe(String username) {
    User user = findUserByUsername(username);
    return userMapper.toDtoUserResponse(user);
  }

  @Override
  @Transactional
  public DtoUserResponse updateMe(String username, DtoUserUpdate dto) {
    User user = findUserByUsername(username);

    // Username benzersizlik kontrolü
    if (dto.getUsername() != null && !dto.getUsername().isBlank()
        && !dto.getUsername().equals(user.getUsername())
        && userRepository.existsByUsername(dto.getUsername())) {
      throw new ConflictException("Username already exists: " + dto.getUsername());
    }

    // Email benzersizlik kontrolü
    if (dto.getEmail() != null && !dto.getEmail().isBlank()
        && !dto.getEmail().equals(user.getEmail())
        && userRepository.existsByEmail(dto.getEmail())) {
      throw new ConflictException("Email already exists: " + dto.getEmail());
    }

    // MapStruct ile sadece null olmayan alanları güncelle
    userMapper.updateUserFromDto(dto, user);

    User savedUser = userRepository.save(user);
    log.info("User profile updated: {}", savedUser.getUsername());
    return userMapper.toDtoUserResponse(savedUser);
  }

  @Override
  @Transactional
  public void changePassword(String username, DtoChangePassword dto) {
    User user = findUserByUsername(username);

    // OAuth kullanıcıları şifre değiştiremez
    if (!"local".equals(user.getProvider())) {
      throw new BadRequestException("Password change is not available for OAuth users");
    }

    // Mevcut şifre doğrulaması
    if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
      throw new BadRequestException("Current password is incorrect");
    }

    // Yeni şifre aynı olmamalı
    if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
      throw new BadRequestException("New password must be different from current password");
    }

    user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
    userRepository.save(user);
    log.info("Password changed for user: {}", username);
  }

  private User findUserByUsername(String username) {
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new BadRequestException("User not found: " + username));
  }
}
