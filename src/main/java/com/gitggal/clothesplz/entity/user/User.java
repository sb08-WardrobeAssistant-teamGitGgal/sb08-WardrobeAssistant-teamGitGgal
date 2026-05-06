package com.gitggal.clothesplz.entity.user;

import com.gitggal.clothesplz.entity.base.BaseUpdatableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseUpdatableEntity {

  @Column(name = "email", nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "name", nullable = false, length = 20)
  private String name;

  @Column(name = "password", nullable = false, length = 255)
  private String password;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", nullable = false, length=20)
  private UserRole role = UserRole.USER;

  @Column(name = "locked", nullable = false)
  private boolean locked = false;

  @Column(name = "temp_password", length = 255)
  private String tempPassword;

  @Column(name = "temp_password_expires_at")
  private Instant tempPasswordExpiresAt;

  public User(String name, String email, String password) {
    this.name = name;
    this.email = email;
    this.password = password;
  }

}
