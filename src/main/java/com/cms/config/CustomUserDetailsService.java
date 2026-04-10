package com.cms.config;

import com.cms.entity.Permission;
import com.cms.entity.Role;
import com.cms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.cms.entity.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(usernameOrEmail)
        .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + usernameOrEmail)));

    return new CustomUserPrincipal(user);
  }

  /**
   * Custom UserDetails implementation that holds the User entity
   */
  public static class CustomUserPrincipal implements UserDetails {
    private final User user;

    public CustomUserPrincipal(User user) {
      this.user = user;
    }

    public User getUser() {
      return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
      List<GrantedAuthority> authorities = new ArrayList<>();

      if (user.getRoles() != null) {
        for (Role role : user.getRoles()) {
          // Role bazlı authority: ROLE_SUPER_ADMIN, ROLE_ADMIN, vb.
          authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

          // Permission bazlı authority: posts:create, pages:read, vb.
          if (role.getPermissions() != null) {
            for (Permission permission : role.getPermissions()) {
              authorities.add(new SimpleGrantedAuthority(permission.getName()));
            }
          }
        }
      }

      return authorities;
    }

    @Override
    public String getPassword() {
      return user.getPassword() != null ? user.getPassword() : "";
    }

    @Override
    public String getUsername() {
      return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
      return true;
    }

    @Override
    public boolean isAccountNonLocked() {
      return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
      return true;
    }

    @Override
    public boolean isEnabled() {
      return user.getIsActive();
    }
  }
}
