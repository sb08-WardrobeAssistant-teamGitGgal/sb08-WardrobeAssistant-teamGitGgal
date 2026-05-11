package com.gitggal.clothesplz.security;

import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.BusinessException;
import com.gitggal.clothesplz.exception.code.UserErrorCode;
import com.gitggal.clothesplz.mapper.user.UserMapper;
import com.gitggal.clothesplz.repository.user.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClothesUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Transactional(readOnly=true)
  @Override
  public UserDetails loadUserByUsername(String email) throws BusinessException{
    User user = userRepository.findByEmail(email)
        .orElseThrow(()-> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    return new ClothesUserDetails(userMapper.toDto(user),user.getPassword());

  }

  @Transactional(readOnly=true)
  public UserDetails loadUserById(UUID userId) throws BusinessException{
    User user = userRepository.findById(userId)
        .orElseThrow(()-> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    return new ClothesUserDetails(userMapper.toDto(user),user.getPassword());

  }
}
