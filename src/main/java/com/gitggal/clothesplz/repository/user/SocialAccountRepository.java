package com.gitggal.clothesplz.repository.user;

import com.gitggal.clothesplz.entity.user.SocialAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {
}
