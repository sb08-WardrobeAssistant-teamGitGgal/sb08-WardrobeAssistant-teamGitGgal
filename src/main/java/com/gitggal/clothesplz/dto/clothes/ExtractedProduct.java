package com.gitggal.clothesplz.dto.clothes;

public record ExtractedProduct(
    String name,
    String imageUrl
) {

  public static ExtractedProduct of(
      String name,
      String imageUrl
  ) {
    return new ExtractedProduct(name, imageUrl);
  }
}