package com.cms.config;

import com.cms.entity.User;
import com.cms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class CustomUserDetailsService implements UserDetailsService {

  @Autowired
  private UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(usernameOrEmail)
        .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail)));

    // OAuth kullanıcıları için password null olabilir, bu durumda boş string kullan
    String password = user.getPassword() != null ? user.getPassword() : "";

    return new org.springframework.security.core.userdetails.User(
        user.getUsername(),
        password,
        new ArrayList<>());
  }
}
