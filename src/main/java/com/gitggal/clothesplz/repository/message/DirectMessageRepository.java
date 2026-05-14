package com.gitggal.clothesplz.repository.message;

import com.gitggal.clothesplz.entity.message.DirectMessage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID>, DirectMessageRepositoryCustom {
}