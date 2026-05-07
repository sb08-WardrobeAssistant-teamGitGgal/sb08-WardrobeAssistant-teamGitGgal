package com.gitggal.clothesplz.service.image;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ImageErrorCode;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class ImageFileValidator {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");

  public void validate(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      log.error("[Service] 이미지 업로드 실패: 이미지가 null 또는 비어있음");
      throw new BusinessException(ImageErrorCode.IMAGE_EMPTY);
    }

    String contentType = image.getContentType();
    if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
      log.error("[Service] 이미지 업로드 실패: 이미지 타입만 업로드 할 수 있음");
      throw new BusinessException(ImageErrorCode.INVALID_IMAGE_CONTENT_TYPE);
    }

    String originalFilename = image.getOriginalFilename();
    if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
      log.error("[Service] 이미지 업로드 실패: 이미지 파일 형식이 확인되지 않음");
      throw new BusinessException(ImageErrorCode.IMAGE_EXTENSION_NOT_FOUND);
    }

    if (!ALLOWED_EXTENSIONS.contains(extractExtension(originalFilename))) {
      log.error("[Service] 이미지 업로드 실패: 지원하지 않는 이미지 파일 형식");
      throw new BusinessException(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT);
    }
  }

  public String extractExtension(String filename) {
    return filename.substring(filename.lastIndexOf('.')).toLowerCase();
  }
}
