package com.gitggal.clothesplz.security;

import com.gitggal.clothesplz.dto.user.UserDto;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@RequiredArgsConstructor
public class ClothesUserDetails implements UserDetails {

  private final UserDto userDto;
  private final String password;
  private final String tempPassword;
  private final Instant tempPasswordExpiresAt;

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + userDto.role().name()));
  }

  @Override
  public String getPassword() {
    if (tempPassword != null
        && tempPasswordExpiresAt != null
        && Instant.now().isBefore(tempPasswordExpiresAt)) {
      return tempPassword;
    }
    return password;
  }

  @Override
  public String getUsername() {
    return userDto.email();
  }
}
