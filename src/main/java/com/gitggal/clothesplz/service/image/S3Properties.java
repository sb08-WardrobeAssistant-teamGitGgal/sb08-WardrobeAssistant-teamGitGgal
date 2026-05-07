package com.gitggal.clothesplz.service.image;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "storage.s3")
public record S3Properties(
    @NotBlank
    String accessKey,

    @NotBlank
    String secretKey,

    @NotBlank
    String region,

    @NotBlank
    String bucket,

    @NotBlank
    String cloudfront
) {

}
