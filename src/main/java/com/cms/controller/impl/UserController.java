package com.cms.controller.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.controller.IUserController;
import com.cms.dto.DtoChangePassword;
import com.cms.dto.DtoUserPermissions;
import com.cms.dto.DtoUserResponse;
import com.cms.dto.DtoUserUpdate;
import com.cms.entity.RootEntityResponse;
import com.cms.service.IUserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController extends BaseController implements IUserController {

  private final IUserService userService;

  @Override
  @GetMapping("/me")
  public RootEntityResponse<DtoUserResponse> getMe() {
    String username = getCurrentUsername();
    return ok(userService.getMe(username));
  }

  @Override
  @PutMapping("/me")
  public RootEntityResponse<DtoUserResponse> updateMe(@Valid @RequestBody DtoUserUpdate dto) {
    String username = getCurrentUsername();
    return ok(userService.updateMe(username, dto));
  }

  @Override
  @PutMapping("/me/password")
  public RootEntityResponse<Void> changePassword(@Valid @RequestBody DtoChangePassword dto) {
    String username = getCurrentUsername();
    userService.changePassword(username, dto);
    return ok(null);
  }

  @Override
  @GetMapping("/me/permissions")
  public RootEntityResponse<DtoUserPermissions> getMyPermissions() {
    String username = getCurrentUsername();
    return ok(userService.getMyPermissions(username));
  }

  private String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof UserDetails userDetails) {
      return userDetails.getUsername();
    }
    throw new RuntimeException("User not authenticated");
  }
}

