package com.cms.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.config.DataSourceConfig;
import com.cms.config.TenantContext;
import com.cms.config.UserAuthCacheService;
import com.cms.dto.DtoAdminUserCreate;
import com.cms.dto.DtoAdminUserUpdate;
import com.cms.dto.DtoUserResponse;
import com.cms.entity.User;
import com.cms.exception.BadRequestException;
import com.cms.exception.ConflictException;
import com.cms.mapper.UserMapper;
import com.cms.repository.UserRepository;
import com.cms.service.ITenantUserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantUserService implements ITenantUserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final UserMapper userMapper;
  private final UserAuthCacheService userAuthCacheService;
  private final DataSourceConfig.TenantDataSourceProperties tenantProperties;

  @Override
  @Transactional
  public DtoUserResponse createUser(String tenantId, DtoAdminUserCreate dto) {
    return inTenantContext(tenantId, () -> {
      if (userRepository.existsByEmail(dto.getEmail())) {
        throw new ConflictException("Email already exists: " + dto.getEmail());
      }
      if (userRepository.existsByUsername(dto.getUsername())) {
        throw new ConflictException("Username already exists: " + dto.getUsername());
      }

      User user = new User();
      user.setUsername(dto.getUsername());
      user.setEmail(dto.getEmail());
      user.setPassword(passwordEncoder.encode(dto.getPassword()));
      user.setFirstName(dto.getFirstName());
      user.setLastName(dto.getLastName());
      user.setProvider("local");
      user.setIsActive(true);
      user.setTokenVersion(1L);

      User saved = userRepository.save(user);
      log.info("Tenant user created: tenant={}, username={}", tenantId, saved.getUsername());
      return userMapper.toDtoUserResponse(saved);
    });
  }

  @Override
  public List<DtoUserResponse> getAllUsers(String tenantId) {
    return inTenantContext(tenantId, () ->
        userRepository.findAll().stream()
            .map(userMapper::toDtoUserResponse)
            .collect(Collectors.toList())
    );
  }

  @Override
  public DtoUserResponse getUserById(String tenantId, Long id) {
    return inTenantContext(tenantId, () -> {
      User user = userRepository.findById(id)
          .orElseThrow(() -> new BadRequestException("User not found with id: " + id));
      return userMapper.toDtoUserResponse(user);
    });
  }

  @Override
  @Transactional
  public DtoUserResponse updateUser(String tenantId, Long id, DtoAdminUserUpdate dto) {
    return inTenantContext(tenantId, () -> {
      User user = userRepository.findById(id)
          .orElseThrow(() -> new BadRequestException("User not found with id: " + id));

      if (dto.getUsername() != null && !dto.getUsername().isBlank()
          && !dto.getUsername().equals(user.getUsername())
          && userRepository.existsByUsername(dto.getUsername())) {
        throw new ConflictException("Username already exists: " + dto.getUsername());
      }

      if (dto.getEmail() != null && !dto.getEmail().isBlank()
          && !dto.getEmail().equals(user.getEmail())
          && userRepository.existsByEmail(dto.getEmail())) {
        throw new ConflictException("Email already exists: " + dto.getEmail());
      }

      if (dto.getUsername() != null && !dto.getUsername().isBlank()) user.setUsername(dto.getUsername());
      if (dto.getEmail() != null && !dto.getEmail().isBlank()) user.setEmail(dto.getEmail());
      if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
      if (dto.getLastName() != null) user.setLastName(dto.getLastName());
      if (dto.getIsActive() != null) user.setIsActive(dto.getIsActive());

      User saved = userRepository.save(user);
      userAuthCacheService.evictUserCache(saved.getUsername());
      log.info("Tenant user updated: tenant={}, id={}", tenantId, id);
      return userMapper.toDtoUserResponse(saved);
    });
  }

  @Override
  @Transactional
  public void deleteUser(String tenantId, Long id) {
    inTenantContext(tenantId, () -> {
      User user = userRepository.findById(id)
          .orElseThrow(() -> new BadRequestException("User not found with id: " + id));
      userAuthCacheService.evictUserCache(user.getUsername());
      userRepository.delete(user);
      log.info("Tenant user deleted: tenant={}, id={}", tenantId, id);
      return null;
    });
  }

  @Override
  @Transactional
  public DtoUserResponse setUserStatus(String tenantId, Long id, boolean isActive) {
    return inTenantContext(tenantId, () -> {
      User user = userRepository.findById(id)
          .orElseThrow(() -> new BadRequestException("User not found with id: " + id));
      user.setIsActive(isActive);
      User saved = userRepository.save(user);
      userAuthCacheService.evictUserCache(saved.getUsername());
      log.info("Tenant user status changed: tenant={}, id={}, active={}", tenantId, id, isActive);
      return userMapper.toDtoUserResponse(saved);
    });
  }

  private void validateTenantId(String tenantId) {
    if (!tenantProperties.getDatasources().containsKey(tenantId)) {
      throw new BadRequestException("Tenant not found: " + tenantId);
    }
  }

  private <T> T inTenantContext(String tenantId, java.util.function.Supplier<T> action) {
    validateTenantId(tenantId);
    String originalTenant = TenantContext.getTenantId();
    try {
      TenantContext.setTenantId(tenantId);
      return action.get();
    } finally {
      TenantContext.setTenantId(originalTenant);
    }
  }
}
