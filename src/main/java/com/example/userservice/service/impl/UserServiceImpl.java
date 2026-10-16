package com.example.userservice.service.impl;

import com.example.userservice.config.CacheNames;
import com.example.userservice.dto.UserDto;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DuplicateResourceException;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.specification.UserSpecifications;
import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

  private static final String RESOURCE = "User";

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  @Transactional
  public UserDto create(UserDto dto) {
    if (userRepository.existsByEmail(dto.email())) {
      throw new DuplicateResourceException(RESOURCE, "email");
    }
    return userMapper.toDto(userRepository.save(userMapper.toEntity(dto)));
  }

  @Override
  @Cacheable(cacheNames = CacheNames.USERS, key = "#id")
  public UserDto getById(Long id) {
    return userRepository.findByIdWithCards(id)
        .map(userMapper::toDto)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
  }

  @Override
  public Page<UserDto> getAll(String name, String surname, Pageable pageable) {
    return userRepository.findAll(UserSpecifications.byFilter(name, surname), pageable)
        .map(userMapper::toDto);
  }

  @Override
  @Transactional
  @CachePut(cacheNames = CacheNames.USERS, key = "#id")
  public UserDto update(Long id, UserDto dto) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    if (userRepository.existsByEmailAndIdNot(dto.email(), id)) {
      throw new DuplicateResourceException(RESOURCE, "email");
    }
    userMapper.updateEntity(dto, user);
    return userMapper.toDto(userRepository.saveAndFlush(user));
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheNames.USERS, key = "#id")
  public void activate(Long id) {
    changeStatus(id, true);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheNames.USERS, key = "#id")
  public void deactivate(Long id) {
    changeStatus(id, false);
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheNames.USERS, key = "#id")
  public void delete(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    userRepository.delete(user);
  }

  private void changeStatus(Long id, boolean active) {
    if (userRepository.updateActiveById(id, active) == 0) {
      throw new ResourceNotFoundException(RESOURCE, id);
    }
  }
}
