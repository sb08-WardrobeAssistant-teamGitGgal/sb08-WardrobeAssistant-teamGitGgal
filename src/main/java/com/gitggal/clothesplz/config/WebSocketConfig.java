package com.gitggal.clothesplz.config;

import com.gitggal.clothesplz.security.websocket.StompAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP 기반 WebSocket 설정
 *
 * 엔드포인트:           /ws
 * 발행 prefix:         /pub -> @MessageMapping로 받는다.
 * 구독 prefix:         /sub -> SimpleBroker가 처리한다.
 * JWT 인증 인터셉터:   CONNECT 프레임의 Authorization 헤더로 JWT 검증
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

  private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

  /**
   * 클라이언트가 처음 WebSocket 연결을 맺는 주소를 등록하는 메서드
   */
  @Override
  public void registerStompEndpoints(StompEndpointRegistry registry) {
    registry
        .addEndpoint("/ws")
        .setAllowedOriginPatterns("*");           // 추후 실제 도메인으로 변경할 예정
  }

  /**
   * 메시지가 어떤 경로로 흐를지 규칙을 정하는 메서드
   */
  @Override
  public void configureMessageBroker(MessageBrokerRegistry registry) {
    // 클라이언트 -> 서버 발행 prefix
    registry.setApplicationDestinationPrefixes("/pub");

    // 서버 -> 클라이언트 구독 prefix
    registry.enableSimpleBroker("/sub");
  }


  /**
   * 클라이언트에서 오는 모든 STOMP 메시지(CONNECT, SEND, SUBSCRIBE 등)가
   * 해당 메서드에서 지정한 인터셉터를 통하게 설정하는 부분
   */
  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    // 클라이언트 -> 서버 채널의 인증 인터셉터
    registration.interceptors(stompAuthChannelInterceptor);
  }
}
