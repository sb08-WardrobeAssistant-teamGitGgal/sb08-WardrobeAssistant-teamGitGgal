package com.gitggal.clothesplz.service.auth;

import com.gitggal.clothesplz.security.jwt.JwtInformation;

public interface AuthService {

  JwtInformation refresh(String refreshToken);
}
