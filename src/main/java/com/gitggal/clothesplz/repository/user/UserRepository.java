package com.gitggal.clothesplz.repository.user;

import com.gitggal.clothesplz.entity.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);
}
