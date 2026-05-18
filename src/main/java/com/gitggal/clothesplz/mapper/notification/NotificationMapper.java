package com.gitggal.clothesplz.mapper.notification;

import com.gitggal.clothesplz.dto.notification.NotificationDto;
import com.gitggal.clothesplz.entity.notification.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * 알림 전용 Mapper 인터페이스
 */
@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(source = "receiver.id", target = "receiverId")
  @Mapping(source = "content", target = "content", defaultValue = "")
  NotificationDto toDto(Notification notification);
}
