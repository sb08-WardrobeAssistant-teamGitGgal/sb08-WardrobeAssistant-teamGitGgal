package com.gitggal.clothesplz.repository.message;

import com.gitggal.clothesplz.entity.message.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {
}
