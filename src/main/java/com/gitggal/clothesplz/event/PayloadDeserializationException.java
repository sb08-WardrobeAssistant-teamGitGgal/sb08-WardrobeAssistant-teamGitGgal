package com.gitggal.clothesplz.event;

public class PayloadDeserializationException extends RuntimeException {

  public PayloadDeserializationException(String payload, Throwable cause) {
    super("역직렬화 실패 (payloadLength=" + (payload == null ? 0 : payload.length()) + ")", cause);
  }
}
