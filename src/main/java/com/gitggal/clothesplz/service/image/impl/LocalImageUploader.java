package com.gitggal.clothesplz.service.image.impl;

import com.gitggal.clothesplz.service.image.ImageUploader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Profile("dev")
@Service
@Slf4j
public class LocalImageUploader implements ImageUploader {

  private static final Path IMAGE_PATH = Path.of(".files/");
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

  @Override
  public String upload(MultipartFile image) {
    try {
      Files.createDirectories(IMAGE_PATH);
      byte[] imageBytes = image.getBytes();

      validateImageFile(image);

      String extension = extractExtension(Objects.requireNonNull(image.getOriginalFilename()));
      String savedFileName = UUID.randomUUID() + extension;
      Path savedPath = IMAGE_PATH.resolve(savedFileName);

      Files.write(savedPath, imageBytes);

      return ServletUriComponentsBuilder
          .fromCurrentContextPath()
          .path("/files/")
          .path(savedFileName)
          .toUriString();
    } catch (IOException e) {
      throw new RuntimeException("이미지 저장 실패", e);
    }
  }

  private void validateImageFile(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
    }

    String contentType = image.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
    }

    String originalFilename = image.getOriginalFilename();
    if (originalFilename == null || !originalFilename.contains(".")) {
      throw new IllegalArgumentException("이미지 확장자를 확인할 수 없습니다.");
    }

    if (!ALLOWED_EXTENSIONS.contains(extractExtension(originalFilename))) {
      throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
    }
  }

  private String extractExtension(String filename) {
    return filename.substring(filename.lastIndexOf('.')).toLowerCase();
  }
}
