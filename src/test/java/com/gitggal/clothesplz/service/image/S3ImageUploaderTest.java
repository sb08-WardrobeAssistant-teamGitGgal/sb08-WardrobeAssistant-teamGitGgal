package com.gitggal.clothesplz.service.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ImageErrorCode;
import com.gitggal.clothesplz.service.image.impl.S3ImageUploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@DisplayName("S3ImageUploader 테스트")
class S3ImageUploaderTest {

  private S3Client mockS3Client;
  private ImageSanitizer mockSanitizer;
  private S3ImageUploader uploader;

  private static final String BUCKET = "test-bucket";
  private static final String CLOUDFRONT_DOMAIN = "cdn.example.com";

  @BeforeEach
  void setUp() {
    mockS3Client = mock(S3Client.class);
    mockSanitizer = mock(ImageSanitizer.class);

    S3Properties props = new S3Properties("test-key", "test-secret", "us-east-1", BUCKET, CLOUDFRONT_DOMAIN);
    uploader = new S3ImageUploader(props, mockSanitizer);
    ReflectionTestUtils.setField(uploader, "s3Client", mockS3Client);
  }

  @Test
  @DisplayName("업로드 성공 시 CloudFront URL을 반환한다")
  void upload_success() {
    byte[] bytes = new byte[]{1, 2, 3};
    ValidatedImage validatedImage = new ValidatedImage(bytes, ".jpg", "image/jpeg");
    MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", bytes);

    when(mockSanitizer.sanitize(file)).thenReturn(validatedImage);
    when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder().build());

    String result = uploader.upload(file);

    assertThat(result).startsWith("https://" + CLOUDFRONT_DOMAIN + "/files/");
    assertThat(result).endsWith(".jpg");
  }

  @Test
  @DisplayName("S3 업로드 실패 시 IMAGE_UPLOAD_FAILED 예외가 발생한다")
  void upload_s3Exception_throws() {
    byte[] bytes = new byte[]{1, 2, 3};
    ValidatedImage validatedImage = new ValidatedImage(bytes, ".jpg", "image/jpeg");
    MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", bytes);

    when(mockSanitizer.sanitize(file)).thenReturn(validatedImage);
    when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(new RuntimeException("S3 error"));

    Throwable thrown = catchThrowable(() -> uploader.upload(file));

    assertThat(thrown).isInstanceOf(BusinessException.class);
    assertThat(((BusinessException) thrown).getErrorCode()).isEqualTo(ImageErrorCode.IMAGE_UPLOAD_FAILED);
  }

  @Test
  @DisplayName("빈 URL로 삭제 요청 시 S3 호출 없이 무시된다")
  void delete_blankUrl_skips() {
    uploader.delete("   ");

    verify(mockS3Client, never()).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  @DisplayName("유효한 URL로 삭제 요청 시 S3 deleteObject를 호출한다")
  void delete_validUrl_success() {
    String imageUrl = "https://" + CLOUDFRONT_DOMAIN + "/files/test-image.jpg";

    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(DeleteObjectResponse.builder().build());

    uploader.delete(imageUrl);

    verify(mockS3Client).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  @DisplayName("S3 삭제 실패 시 예외가 전파되지 않는다")
  void delete_s3Exception_noThrow() {
    String imageUrl = "https://" + CLOUDFRONT_DOMAIN + "/files/test-image.jpg";

    doThrow(new RuntimeException("S3 error")).when(mockS3Client).deleteObject(any(DeleteObjectRequest.class));

    Throwable thrown = catchThrowable(() -> uploader.delete(imageUrl));

    assertThat(thrown).isNull();
  }
}
