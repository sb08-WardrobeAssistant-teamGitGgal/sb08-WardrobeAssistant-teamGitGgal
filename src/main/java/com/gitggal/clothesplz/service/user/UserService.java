package com.gitggal.clothesplz.service.user;

import com.gitggal.clothesplz.dto.user.ChangePasswordRequest;
import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.dto.user.UserDtoCursorRequest;
import com.gitggal.clothesplz.dto.user.UserDtoCursorResponse;
import com.gitggal.clothesplz.dto.user.UserRoleUpdateRequest;
import java.util.UUID;

public interface UserService {

  UserDto create(UserCreateRequest request);

  void updatePassword(UUID userId, ChangePasswordRequest request);

  UserDto updateRole(UUID userId, UserRoleUpdateRequest request);

  UserDtoCursorResponse findAll(UserDtoCursorRequest request);
}
