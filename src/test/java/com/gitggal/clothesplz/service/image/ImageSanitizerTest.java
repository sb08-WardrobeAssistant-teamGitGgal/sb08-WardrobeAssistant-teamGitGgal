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

  private byte[] createJpegBytes() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "jpg", baos);
    return baos.toByteArray();
  }

  private byte[] createPngBytes() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), "png", baos);
    return baos.toByteArray();
  }

  @Test
  @DisplayName("유효한 JPEG 이미지는 정상 처리된다")
  void sanitize_validJpeg_success() throws Exception {
    MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", createJpegBytes());

    ValidatedImage result = sanitizer.sanitize(file);

    assertThat(result).isNotNull();
    assertThat(result.contentType()).isEqualTo("image/jpeg");
    assertThat(result.extension()).isEqualTo(".jpg");
  }

  @Test
  @DisplayName("유효한 PNG 이미지는 정상 처리된다")
  void sanitize_validPng_success() throws Exception {
    MockMultipartFile file = new MockMultipartFile("image", "photo.png", "image/png", createPngBytes());

    ValidatedImage result = sanitizer.sanitize(file);

    assertThat(result).isNotNull();
    assertThat(result.contentType()).isEqualTo("image/png");
    assertThat(result.extension()).isEqualTo(".png");
  }

  @Test
  @DisplayName("content-type이 application/octet-stream이어도 실제 JPEG 바이트면 정상 처리된다")
  void sanitize_octetStream_success() throws Exception {
    MockMultipartFile file = new MockMultipartFile("image", "", "application/octet-stream", createJpegBytes());

    ValidatedImage result = sanitizer.sanitize(file);

    assertThat(result.contentType()).isEqualTo("image/jpeg");
  }

  @Test
  @DisplayName("잘못된 content-type 헤더이어도 실제 이미지 바이트면 정상 처리된다")
  void sanitize_wrongContentType_success() throws Exception {
    MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "text/plain", createJpegBytes());

    ValidatedImage result = sanitizer.sanitize(file);

    assertThat(result.contentType()).isEqualTo("image/jpeg");
  }

  @Test
  @DisplayName("파일명이 없어도 바이트 검증으로 정상 처리된다")
  void sanitize_nullFilename_success() throws Exception {
    MockMultipartFile file = new MockMultipartFile("image", null, "image/jpeg", createJpegBytes());

    ValidatedImage result = sanitizer.sanitize(file);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("확장자 없는 파일명이어도 바이트 검증으로 정상 처리된다")
  void sanitize_noExtension_success() throws Exception {
    MockMultipartFile file = new MockMultipartFile("image", "filename", "image/jpeg", createJpegBytes());

    ValidatedImage result = sanitizer.sanitize(file);

    assertThat(result).isNotNull();
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
  @DisplayName("이미지가 아닌 바이트(텍스트)면 INVALID_IMAGE_CONTENT_TYPE 예외가 발생한다")
  void sanitize_nonImageBytes_throws() {
    MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "not-an-image".getBytes());

    Throwable thrown = catchThrowable(() -> sanitizer.sanitize(file));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ImageErrorCode.INVALID_IMAGE_CONTENT_TYPE);
  }

  @Test
  @DisplayName("GIF 등 미지원 이미지 형식이면 UNSUPPORTED_IMAGE_FORMAT 예외가 발생한다")
  void sanitize_unsupportedImageFormat_throws() {
    // GIF magic bytes: GIF89a
    byte[] gifBytes = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00, 0x01, 0x00};
    MockMultipartFile file = new MockMultipartFile("image", "file.gif", "image/gif", gifBytes);

    Throwable thrown = catchThrowable(() -> sanitizer.sanitize(file));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode())
        .isEqualTo(ImageErrorCode.UNSUPPORTED_IMAGE_FORMAT);
  }
}
