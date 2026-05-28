package com.gitggal.clothesplz.service.image;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ImageErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class ImageSanitizer {

  // WebP는 순수 Java ImageIO 기반 writer 없음 → JPEG로 변환 저장
  private static final Map<String, ImageType> MIME_TO_OUTPUT_TYPE = Map.of(
      "image/jpeg", ImageType.JPEG,
      "image/png", ImageType.PNG,
      "image/webp", ImageType.JPEG
  );

  // Apache Tika 라이브러리
  private static final Tika TIKA = new Tika();

  public ValidatedImage sanitize(MultipartFile image) {
    if (image == null || image.isEmpty()) {
      log.error("[Service] 이미지 업로드 실패: 이미지가 null 또는 비어있음");
      throw new BusinessException(ImageErrorCode.IMAGE_EMPTY);
    }

    try {
      byte[] imageBytes = image.getBytes();
      String detectedMimeType = TIKA.detect(imageBytes);

      if (!MIME_TO_OUTPUT_TYPE.containsKey(detectedMimeType)) {
        log.error("[Service] 이미지 업로드 실패: 지원하지 않는 파일 형식 = {}", detectedMimeType);
        throw new BusinessException(detectedMimeType.startsWith("image/")
            ? ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT
            : ImageErrorCode.INVALID_IMAGE_CONTENT_TYPE);
      }

      ImageType imageType = MIME_TO_OUTPUT_TYPE.get(detectedMimeType);
      BufferedImage decodedImage = ImageIO.read(new ByteArrayInputStream(imageBytes)); // 디코딩

      if (decodedImage == null) {
        log.error("[Service] 이미지 업로드 실패: 이미지 디코딩 실패");
        throw new BusinessException(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT);
      }

      byte[] reEncoded = reEncode(decodedImage, imageType);
      return new ValidatedImage(reEncoded, imageType.getExtension(), imageType.getContentType());
    } catch (IOException e) {
      log.error("[Service] 이미지 업로드 실패: 이미지 검증 중 예외 발생", e);
      throw new BusinessException(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT, e);
    }
  }

  private byte[] reEncode(BufferedImage image, ImageType imageType) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    if (!ImageIO.write(image, imageType.getWriterFormat(), outputStream)) {
      log.error("[Service] 이미지 업로드 실패: 이미지 재인코딩 실패");
      throw new BusinessException(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT);
    }
    return outputStream.toByteArray();
  }
}
