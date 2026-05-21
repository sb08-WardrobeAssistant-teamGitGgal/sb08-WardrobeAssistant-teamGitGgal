package com.gitggal.clothesplz.repository.user;

import com.gitggal.clothesplz.entity.user.SocialAccount;
import com.gitggal.clothesplz.entity.user.SocialProvider;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, UUID> {

  Optional<SocialAccount> findByProviderAndProviderId(SocialProvider provider, String providerId);
}
