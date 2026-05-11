package com.gitggal.clothesplz.security.jwt;

import com.gitggal.clothesplz.dto.user.UserDto;

public record JwtDto(
    UserDto userDto,
    String accessToken
) {

}
