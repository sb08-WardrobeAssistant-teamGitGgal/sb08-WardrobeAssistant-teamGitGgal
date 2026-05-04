package com.gitggal.clothesplz.repository.user;

import com.gitggal.clothesplz.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
}
