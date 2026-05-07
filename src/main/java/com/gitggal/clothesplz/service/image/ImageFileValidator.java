package com.gitggal.clothesplz.service.image;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ImageErrorCode;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageFileValidator {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

  public void validate(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      throw new BusinessException(ImageErrorCode.IMAGE_EMPTY);
    }

    String contentType = image.getContentType();
    if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
      throw new BusinessException(ImageErrorCode.INVALID_IMAGE_CONTENT_TYPE);
    }

    String originalFilename = image.getOriginalFilename();
    if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
      throw new BusinessException(ImageErrorCode.IMAGE_EXTENSION_NOT_FOUND);
    }

    if (!ALLOWED_EXTENSIONS.contains(extractExtension(originalFilename))) {
      throw new BusinessException(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT);
    }
  }

  public String extractExtension(String filename) {
    return filename.substring(filename.lastIndexOf('.')).toLowerCase();
  }
}
