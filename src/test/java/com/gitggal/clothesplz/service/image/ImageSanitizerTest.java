package com.gitggal.clothesplz.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ImageErrorCode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("ImageSanitizer 테스트")
class ImageSanitizerTest {

  private final ImageSanitizer sanitizer = new ImageSanitizer();

  @Test
  @DisplayName("application/octet-stream 타입 파일은 파일명 검사를 건너뛰고 바이트 검증으로 처리된다")
  void sanitize_octetStream_success() throws Exception {
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", baos);
    byte[] jpegBytes = baos.toByteArray();

    MockMultipartFile file = new MockMultipartFile(
        "image", "", "application/octet-stream", jpegBytes
    );

    ValidatedImage result = sanitizer.sanitize(file);

    assertThat(result).isNotNull();
    assertThat(result.contentType()).isEqualTo("image/jpeg");
  }

  @Test
  @DisplayName("image/가 아닌 타입이면 INVALID_IMAGE_CONTENT_TYPE 예외가 발생한다")
  void sanitize_invalidContentType_throws() {
    MockMultipartFile file = new MockMultipartFile(
        "image", "test.jpg", "text/plain", "data".getBytes()
    );

    Throwable thrown = catchThrowable(() -> sanitizer.sanitize(file));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ImageErrorCode.INVALID_IMAGE_CONTENT_TYPE);
  }
}
