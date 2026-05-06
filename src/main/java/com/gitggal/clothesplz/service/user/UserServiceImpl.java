package com.gitggal.clothesplz.service.user;

import com.gitggal.clothesplz.dto.user.UserCreateRequest;
import com.gitggal.clothesplz.dto.user.UserDto;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.exception.user.DuplicateEmailException;
import com.gitggal.clothesplz.mapper.user.UserMapper;
import com.gitggal.clothesplz.repository.user.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Override
  @Transactional
  public UserDto create(UserCreateRequest request) {
    String name = request.name();
    String email = request.email();

    if (userRepository.existsByEmail(email)) {
      throw new DuplicateEmailException(email);
    }

    String password = passwordEncoder.encode(request.password());

    User user = new User(name, email, password);

    try {
      User savedUser = userRepository.save(user);
      return userMapper.toDto(savedUser);
    } catch (DataIntegrityViolationException e) {
      throw new DuplicateEmailException(email);
    }
  }
}
