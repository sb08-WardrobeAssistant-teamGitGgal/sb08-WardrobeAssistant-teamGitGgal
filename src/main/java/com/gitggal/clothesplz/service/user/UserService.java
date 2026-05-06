package com.gitggal.clothesplz.service.user;

import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;

public interface UserService {
  UserDto create(UserCreateRequest request);
}
