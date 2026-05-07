package com.gitggal.clothesplz.service.image.impl;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ImageErrorCode;
import com.gitggal.clothesplz.service.image.ImageFileValidator;
import com.gitggal.clothesplz.service.image.ImageUploader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Profile("dev")
@Service
@Slf4j
@RequiredArgsConstructor
public class LocalImageUploader implements ImageUploader {

  private static final Path IMAGE_PATH = Path.of(".files/");
  private final ImageFileValidator imageFileValidator;

  @Override
  public String upload(MultipartFile image) {
    log.info("[Service] 이미지 업로드 요청 시작: 요청 파일 명 = {}", image.getOriginalFilename());
    imageFileValidator.validate(image);

    try {
      Files.createDirectories(IMAGE_PATH);
      byte[] imageBytes = image.getBytes();

      String extension = imageFileValidator.extractExtension(
          Objects.requireNonNull(image.getOriginalFilename()));
      String savedFileName = UUID.randomUUID() + extension;
      Path savedPath = IMAGE_PATH.resolve(savedFileName);

      Files.write(savedPath, imageBytes);

      log.info("[Service] 이미지 업로드 요청 완료: 저장 파일 명 = {}", savedFileName);
      return ServletUriComponentsBuilder
          .fromCurrentContextPath()
          .path("/files/")
          .path(savedFileName)
          .toUriString();
    } catch (IOException e) {
      log.error("[Service] 이미지 업로드 요청 실패: {}", e.getMessage(), e);
      throw new BusinessException(ImageErrorCode.IMAGE_UPLOAD_FAILED, e);
    }
  }

  @Override
  public void delete(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      return;
    }
    log.info("[Service] 이미지 삭제 요청 시작");

    try {
      String fileName = Path.of(URI.create(imageUrl).getPath()).getFileName().toString();
      Path filePath = IMAGE_PATH.resolve(fileName);
      Files.deleteIfExists(filePath);
      log.info("[Service] 이미지 삭제 요청 완료");
    } catch (Exception e) {
      log.warn("[Service] 이미지 삭제 요청 실패: imageUrl={}, reason={}", imageUrl, e.getMessage());
    }
  }
}
