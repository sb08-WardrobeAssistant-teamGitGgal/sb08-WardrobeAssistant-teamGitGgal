package com.gitggal.clothesplz.service.image;

import lombok.Getter;

@Getter
public enum ImageType {
  JPEG(".jpg", "image/jpeg", "jpg"),
  PNG(".png", "image/png", "png");

  private final String extension;
  private final String contentType;
  private final String writerFormat;

  ImageType(String extension, String contentType, String writerFormat) {
    this.extension = extension;
    this.contentType = contentType;
    this.writerFormat = writerFormat;
  }
}
