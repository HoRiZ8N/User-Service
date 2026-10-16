package com.example.userservice.service;

import com.example.userservice.dto.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

  UserDto create(UserDto dto);

  UserDto getById(Long id);

  Page<UserDto> getAll(String name, String surname, Pageable pageable);

  UserDto update(Long id, UserDto dto);

  void activate(Long id);

  void deactivate(Long id);

  void delete(Long id);
}
