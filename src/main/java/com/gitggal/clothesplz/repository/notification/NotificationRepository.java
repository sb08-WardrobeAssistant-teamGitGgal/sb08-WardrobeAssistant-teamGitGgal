package com.gitggal.clothesplz.repository.notification;

import com.gitggal.clothesplz.entity.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}
