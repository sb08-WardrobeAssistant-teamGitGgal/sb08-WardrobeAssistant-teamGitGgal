package com.gitggal.clothesplz.service.image.impl;

import com.gitggal.clothesplz.service.image.ImageUploader;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Profile("dev")
@Service
public class LocalImageUploader implements ImageUploader {

  @Override
  public String upload(MultipartFile image) {
    return "";
  }
}
