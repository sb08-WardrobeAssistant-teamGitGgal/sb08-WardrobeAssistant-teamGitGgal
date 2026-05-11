package com.gitggal.clothesplz.mapper.user;

import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

  UserDto toDto(User user);
}
