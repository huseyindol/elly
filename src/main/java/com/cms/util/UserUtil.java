package com.cms.util;

import org.springframework.stereotype.Component;

import com.cms.entity.User;

/**
 * User entity ile ilgili utility metodları
 */
@Component
public class UserUtil {

  /**
   * UserCode oluşturur: firstName ve lastName'in ilk harflerini birleştirir
   * Örnek: "Hüseyin" + "Dol" -> "HD"
   * 
   * @param user User entity
   * @return UserCode string (büyük harf)
   */
  public String generateUserCode(User user) {
    if (user == null) {
      return "";
    }

    StringBuilder code = new StringBuilder();

    // FirstName'in ilk harfini al
    if (user.getFirstName() != null && !user.getFirstName().trim().isEmpty()) {
      code.append(user.getFirstName().trim().charAt(0));
    }

    // LastName'in ilk harfini al
    if (user.getLastName() != null && !user.getLastName().trim().isEmpty()) {
      code.append(user.getLastName().trim().charAt(0));
    }

    // Eğer hiçbir şey yoksa username'in ilk harfini kullan
    if (code.length() == 0 && user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
      code.append(user.getUsername().trim().charAt(0));
    }

    return code.toString().toUpperCase();
  }
}
