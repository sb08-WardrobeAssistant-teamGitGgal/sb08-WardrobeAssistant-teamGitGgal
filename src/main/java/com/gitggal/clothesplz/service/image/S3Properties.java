package com.gitggal.clothesplz.service.image;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record S3Properties(
    String accessKey,
    String secretKey,
    String region,
    String bucket,
    String cloudfront
) {

}
