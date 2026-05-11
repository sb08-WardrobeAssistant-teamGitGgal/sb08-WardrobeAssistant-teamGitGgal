package com.gitggal.clothesplz.service.image;

public record ValidatedImage(
    byte[] bytes,
    String extension,
    String contentType
) {

}
