package com.gitggal.clothesplz.service.image.impl;

import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.ImageErrorCode;
import com.gitggal.clothesplz.service.image.ImageFileValidator;
import com.gitggal.clothesplz.service.image.ImageUploader;
import com.gitggal.clothesplz.service.image.S3Properties;
import java.net.URI;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Profile("prod")
@Service
@Slf4j
public class S3ImageUploader implements ImageUploader {

  private static final String FILE_PREFIX = "files/";

  private final S3Client s3Client;
  private final String bucket;
  private final String cloudfrontDomain;
  private final ImageFileValidator imageFileValidator;

  public S3ImageUploader(S3Properties properties, ImageFileValidator imageFileValidator) {
    this.bucket = properties.bucket();
    this.cloudfrontDomain = properties.cloudfront();
    this.imageFileValidator = imageFileValidator;

    // Region 설정: ap-northeast-2
    S3ClientBuilder builder = S3Client.builder()
        .region(Region.of(properties.region()));

    // AccessKey, SecretKey 설정
    if (StringUtils.hasText(properties.accessKey()) && StringUtils.hasText(properties.secretKey())) {
      builder.credentialsProvider(StaticCredentialsProvider.create(
          AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())));
    } else {
      builder.credentialsProvider(DefaultCredentialsProvider.builder().build());
    }

    this.s3Client = builder.build();
  }

  @Override
  public String upload(MultipartFile image) {
    log.info("[Service] S3 이미지 업로드 요청 시작: 요청 파일 명 = {}", image.getOriginalFilename());
    imageFileValidator.validate(image);

    String extension = imageFileValidator.extractExtension(
        Objects.requireNonNull(image.getOriginalFilename()));
    String objectKey = FILE_PREFIX + UUID.randomUUID() + extension;

    try {
      PutObjectRequest request = PutObjectRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .contentType(image.getContentType())
          .build();

      s3Client.putObject(request, RequestBody.fromBytes(image.getBytes()));
      log.info("[Service] S3 이미지 업로드 요청 완료: 저장 파일 명 = {}", objectKey);
      return buildCloudfrontUrl(objectKey);
    } catch (Exception e) {
      log.error("[Service] S3 이미지 업로드 실패: {}", e.getMessage(), e);
      throw new BusinessException(ImageErrorCode.IMAGE_UPLOAD_FAILED, e);
    }
  }

  @Override
  public void delete(String imageUrl) {
    if (!StringUtils.hasText(imageUrl)) {
      return;
    }

    log.info("[Service] S3 이미지 삭제 요청 시작: 이미지 URL = {}", imageUrl);
    String objectKey = extractObjectKey(imageUrl);
    if (!StringUtils.hasText(objectKey)) {
      return;
    }

    try {
      DeleteObjectRequest request = DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(objectKey)
          .build();
      s3Client.deleteObject(request);
    } catch (Exception e) {
      log.warn("[Service] S3 이미지 삭제 실패: {}", e.getMessage());
    }
  }

  private String buildCloudfrontUrl(String objectKey) {
    return "https://" + cloudfrontDomain + "/" + objectKey;
  }

  private String extractObjectKey(String imageUrl) {
    try {
      URI uri = URI.create(imageUrl);
      String path = uri.getPath();

      if (!StringUtils.hasText(path)) {
        return null;
      }

      return path.startsWith("/") ? path.substring(1) : path;
    } catch (Exception e) {
      log.warn("[Service] 이미지 URL 파싱 실패: {}", imageUrl);
      return null;
    }
  }
}
