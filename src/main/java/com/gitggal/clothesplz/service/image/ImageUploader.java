package com.gitggal.clothesplz.service.image;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploader {

  String upload(MultipartFile image);
}
