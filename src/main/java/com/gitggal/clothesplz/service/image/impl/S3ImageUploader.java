package com.gitggal.clothesplz.service.image.impl;

import com.gitggal.clothesplz.exception.code.ImageErrorCode;
import com.gitggal.clothesplz.exception.image.ImageUploadFailedException;
import com.gitggal.clothesplz.service.image.ImageUploader;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Profile("prod")
@Service
public class S3ImageUploader implements ImageUploader {

  @Override
  public String upload(MultipartFile image) {
    throw new ImageUploadFailedException(ImageErrorCode.IMAGE_UPLOAD_FAILED);
  }

  @Override
  public void delete(String imageUrl) {
    // TODO: S3 연동 시 실제 삭제 로직 구현
  }
}
