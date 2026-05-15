package com.gitggal.clothesplz.repository.user;

import com.gitggal.clothesplz.dto.user.UserDtoCursorRequest;
import com.gitggal.clothesplz.entity.user.User;
import java.util.List;

public interface UserRepositoryCustom {

  List<User> getAllUsers(UserDtoCursorRequest request);

}
