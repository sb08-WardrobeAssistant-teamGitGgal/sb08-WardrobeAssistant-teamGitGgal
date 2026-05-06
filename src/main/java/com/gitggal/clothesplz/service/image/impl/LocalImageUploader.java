package com.gitggal.clothesplz.service.image.impl;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ImageErrorCode;
import com.gitggal.clothesplz.service.image.ImageUploader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
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
    validateImageFile(image);

    try {
      Files.createDirectories(IMAGE_PATH);
      byte[] imageBytes = image.getBytes();

      String extension = extractExtension(image.getOriginalFilename());
      String savedFileName = UUID.randomUUID() + extension;
      Path savedPath = IMAGE_PATH.resolve(savedFileName);

      Files.write(savedPath, imageBytes);

      return ServletUriComponentsBuilder
          .fromCurrentContextPath()
          .path("/files/")
          .path(savedFileName)
          .toUriString();
    } catch (IOException e) {
      log.error("이미지 업로드 실패: {}", e.getMessage(), e);
      throw new BusinessException(ImageErrorCode.IMAGE_UPLOAD_FAILED, e);
    }
  }

  @Override
  public void delete(String imageUrl) {
    if (imageUrl == null || imageUrl.isBlank()) {
      return;
    }

    try {
      String fileName = Path.of(URI.create(imageUrl).getPath()).getFileName().toString();
      Path filePath = IMAGE_PATH.resolve(fileName);
      Files.deleteIfExists(filePath);
    } catch (Exception e) {
      log.warn("이미지 삭제 실패: imageUrl={}, reason={}", imageUrl, e.getMessage());
    }
  }

  private void validateImageFile(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      throw new BusinessException(ImageErrorCode.IMAGE_EMPTY);
    }

    String contentType = image.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new BusinessException(ImageErrorCode.INVALID_IMAGE_CONTENT_TYPE);
    }

    String originalFilename = image.getOriginalFilename();
    if (originalFilename == null || !originalFilename.contains(".")) {
      throw new BusinessException(ImageErrorCode.IMAGE_EXTENSION_NOT_FOUND);
    }

    if (!ALLOWED_EXTENSIONS.contains(extractExtension(originalFilename))) {
      throw new BusinessException(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT);
    }
  }

  private String extractExtension(String filename) {
    return filename.substring(filename.lastIndexOf('.')).toLowerCase();
  }
}
