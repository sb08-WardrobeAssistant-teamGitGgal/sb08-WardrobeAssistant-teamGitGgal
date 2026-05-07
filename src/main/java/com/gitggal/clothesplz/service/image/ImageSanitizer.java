package com.gitggal.clothesplz.service.image;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ImageErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class ImageSanitizer {

  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
  private static final byte[] JPEG_SOI = new byte[]{(byte) 0xFF, (byte) 0xD8};
  private static final byte[] PNG_SIG = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

  public ValidatedImage sanitize(MultipartFile image) {
    // 메타데이터 기반 1차 검증 (content-type, 파일명/확장자: 위조 가능)
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

    // 파일 바이트 기반 2차 검증 (시그니처/디코딩: 우회 난이도 높음)
    try {
      byte[] imageBytes = image.getBytes();
      ImageType imageFormat = detectImageFormat(imageBytes);
      if (imageFormat == null) {
        log.error("[Service] 이미지 업로드 실패: 파일 시그니처 검증 실패");
        throw new BusinessException(ImageErrorCode.INVALID_IMAGE_CONTENT_TYPE);
      }

      BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
      if (decodedImage == null) {
        log.error("[Service] 이미지 업로드 실패: 이미지 디코딩 실패");
        throw new BusinessException(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT);
      }

      byte[] reEncoded = reEncode(decodedImage, imageFormat);

      return new ValidatedImage(
          reEncoded,
          imageFormat.getExtension(),
          imageFormat.getContentType()
      );
    } catch (IOException e) {
      log.error("[Service] 이미지 업로드 실패: 이미지 검증 중 예외 발생", e);
      throw new BusinessException(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT, e);
    }
  }

  public String extractExtension(String filename) {
    return filename.substring(filename.lastIndexOf('.')).toLowerCase();
  }

  private ImageType detectImageFormat(byte[] bytes) {
    if (startsWith(bytes, JPEG_SOI)) {
      return ImageType.JPEG;
    }
    if (startsWith(bytes, PNG_SIG)) {
      return ImageType.PNG;
    }
    return null;
  }

  private boolean startsWith(byte[] source, byte[] prefix) {
    if (source.length < prefix.length) {
      return false;
    }

    for (int i = 0; i < prefix.length; i++) {
      if (source[i] != prefix[i]) {
        return false;
      }
    }

    return true;
  }

  private byte[] reEncode(BufferedImage image, ImageType imageFormat) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    if (!ImageIO.write(image, imageFormat.getWriterFormat(), outputStream)) {
      log.error("[Service] 이미지 업로드 실패: 이미지 재인코딩 실패");
      throw new BusinessException(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT);
    }
    return outputStream.toByteArray();
  }
}
