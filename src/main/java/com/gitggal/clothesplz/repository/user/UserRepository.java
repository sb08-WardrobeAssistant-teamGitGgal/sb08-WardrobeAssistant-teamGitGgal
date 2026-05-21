package com.gitggal.clothesplz.repository.user;

import com.gitggal.clothesplz.entity.user.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID>, UserRepositoryCustom {

  boolean existsByEmail(String email);

  Optional<User> findByEmail(String email);

  @Query("SELECT u.id FROM User u")
  List<UUID> findAllIds();
}
