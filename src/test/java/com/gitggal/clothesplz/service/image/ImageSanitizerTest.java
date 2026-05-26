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

  @Test
  @DisplayName("contentType이 null이면 INVALID_IMAGE_CONTENT_TYPE 예외가 발생한다")
  void sanitize_nullContentType_throws() {
    MockMultipartFile file = new MockMultipartFile(
        "image", "test.jpg", null, "data".getBytes()
    );

    Throwable thrown = catchThrowable(() -> sanitizer.sanitize(file));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ImageErrorCode.INVALID_IMAGE_CONTENT_TYPE);
  }

  @Test
  @DisplayName("originalFilename이 null이면 IMAGE_EXTENSION_NOT_FOUND 예외가 발생한다")
  void sanitize_nullOriginalFilename_throws() {
    MockMultipartFile file = new MockMultipartFile(
        "image", null, "image/jpeg", "data".getBytes()
    );

    Throwable thrown = catchThrowable(() -> sanitizer.sanitize(file));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ImageErrorCode.IMAGE_EXTENSION_NOT_FOUND);
  }

  @Test
  @DisplayName("이미지가 비어있으면 IMAGE_EMPTY 예외가 발생한다")
  void sanitize_emptyFile_throws() {
    MockMultipartFile file = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

    Throwable thrown = catchThrowable(() -> sanitizer.sanitize(file));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode()).isEqualTo(ImageErrorCode.IMAGE_EMPTY);
  }

  @Test
  @DisplayName("확장자가 없으면 IMAGE_EXTENSION_NOT_FOUND 예외가 발생한다")
  void sanitize_filenameWithoutExtension_throws() throws Exception {
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, "jpg", baos);

    MockMultipartFile file = new MockMultipartFile("image", "filename", "image/jpeg", baos.toByteArray());

    Throwable thrown = catchThrowable(() -> sanitizer.sanitize(file));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ImageErrorCode.IMAGE_EXTENSION_NOT_FOUND);
  }

  @Test
  @DisplayName("허용되지 않는 확장자면 UNSUPPORTED_IMAGE_FORMAT 예외가 발생한다")
  void sanitize_unsupportedExtension_throws() {
    MockMultipartFile file = new MockMultipartFile("image", "file.gif", "image/gif", "GIF".getBytes());

    Throwable thrown = catchThrowable(() -> sanitizer.sanitize(file));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT);
  }

  @Test
  @DisplayName("시그니처가 올바르지 않으면 INVALID_IMAGE_CONTENT_TYPE 예외가 발생한다")
  void sanitize_invalidSignature_throws() {
    MockMultipartFile file = new MockMultipartFile("image", "file.jpg", "image/jpeg", "not-image".getBytes());

    Throwable thrown = catchThrowable(() -> sanitizer.sanitize(file));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ImageErrorCode.INVALID_IMAGE_CONTENT_TYPE);
  }
}
