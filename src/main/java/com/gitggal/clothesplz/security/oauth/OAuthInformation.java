package com.gitggal.clothesplz.security.oauth;

import com.gitggal.clothesplz.entity.user.SocialProvider;

public record OAuthInformation(
    SocialProvider provider,
    String providerId,
    String email,
    String nickname
) {

}
