package com.gitggal.clothesplz.security.jwt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "clothesplz.jwt")
public record JwtProperties(

    @NotBlank
    @Size(min = 32, message = "Access token은 32자 이상이어야 합니다.")
    String accessTokenSecret,

    @Min(1000)
    int accessTokenExp,

    @NotBlank
    @Size(min = 32, message = "Refresh token은 32자 이상이어야 합니다.")
    String refreshTokenSecret,

    @Min(1000)
    int refreshTokenExp
) {

}
