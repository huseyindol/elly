package com.cms.service;

import java.util.List;

import com.cms.dto.DtoChangePassword;
import com.cms.dto.DtoCreateUser;
import com.cms.dto.DtoUserPermissions;
import com.cms.dto.DtoUserResponse;
import com.cms.dto.DtoUserUpdate;

public interface IUserService {
  DtoUserResponse getMe(String username);

  DtoUserResponse updateMe(String username, DtoUserUpdate dto);

  void changePassword(String username, DtoChangePassword dto);

  DtoUserPermissions getMyPermissions(String username);

  /**
   * Tüm kullanıcıları listeler.
   * @param audience "panel" → SUPER_ADMIN/ADMIN/EDITOR/VIEWER rollerinden EN AZ birine sahip
   *                 olanlar; "tenant" → TENANT rolüne sahip olanlar; null/blank → tümü.
   */
  List<DtoUserResponse> getAllUsers(String audience);

  DtoUserResponse getUserById(Long id);

  /**
   * SUPER_ADMIN/ADMIN tarafından panel UI'sından kullanıcı (genelde admin) oluşturur.
   * Tenant context çağıran kullanıcının JWT'sinden gelir. roleIds verilmezse ADMIN
   * default'u atanır. Verification mail PANEL frontend URL'i ile gönderilir.
   */
  DtoUserResponse createUser(DtoCreateUser request);

  /**
   * Kullanıcıyı kalıcı siler. Çağıran kullanıcı kendini silemez. Aktif refresh token'lar
   * temizlenir (silinen kullanıcının açık oturumları sonlanır), auth cache invalid edilir.
   */
  void deleteUser(Long id, String currentUsername);

  /**
   * Kullanıcı aktif/pasif durumunu değiştirir. isActive=false olan kullanıcı login yapamaz,
   * mevcut auth cache invalid edilir. Self-status koruması: kendi hesabını pasifleştiremez.
   */
  DtoUserResponse setUserStatus(Long id, boolean isActive, String currentUsername);
}

