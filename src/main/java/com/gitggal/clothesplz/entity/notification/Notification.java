package com.gitggal.clothesplz.entity.notification;

import com.gitggal.clothesplz.entity.base.BaseEntity;
import com.gitggal.clothesplz.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 알림 Entity
 * 필드 구성
 * - receiver      : 알림을 받을 사용자
 * - title         : 알림 제목 (예: "새로운 의상 속성이 추가되었어요.")
 * - content       : 알림 내용 (예: "내 의상에 [사이즈] 속성을 추가해보세요.")
 * - level         : 알림 심각도 (INFO / WARNING / ERROR)
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Notification extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "receiver_id", nullable = false)
  private User receiver;

  @Column(nullable = false, length = 50)
  private String title;

  @Column(length = 100)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private NotificationLevel level;
}
