package com.example.userservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.userservice.dto.UserDto;
import com.example.userservice.entity.User;
import com.example.userservice.exception.DuplicateResourceException;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.mapper.UserMapper;
import com.example.userservice.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  private static final Long USER_ID = 1L;
  private static final String EMAIL = "john@example.com";

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserMapper userMapper;

  @InjectMocks
  private UserServiceImpl userService;

  @Test
  void createSavesUserWhenEmailIsFree() {
    UserDto request = request(EMAIL);
    User user = user();
    UserDto expected = response();
    when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
    when(userMapper.toEntity(request)).thenReturn(user);
    when(userRepository.save(user)).thenReturn(user);
    when(userMapper.toDto(user)).thenReturn(expected);

    UserDto result = userService.create(request);

    assertThat(result).isEqualTo(expected);
    verify(userRepository).save(user);
  }

  @Test
  void createThrowsWhenEmailExists() {
    when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

    assertThatThrownBy(() -> userService.create(request(EMAIL)))
        .isInstanceOf(DuplicateResourceException.class);
    verify(userRepository, never()).save(any());
  }

  @Test
  void getByIdReturnsUserWithCards() {
    User user = user();
    UserDto expected = response();
    when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.of(user));
    when(userMapper.toDto(user)).thenReturn(expected);

    assertThat(userService.getById(USER_ID)).isEqualTo(expected);
  }

  @Test
  void getByIdThrowsWhenUserMissing() {
    when(userRepository.findByIdWithCards(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.getById(USER_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void getAllReturnsMappedPage() {
    Pageable pageable = PageRequest.of(0, 10);
    User user = user();
    UserDto expected = response();
    when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(user), pageable, 1));
    when(userMapper.toDto(user)).thenReturn(expected);

    Page<UserDto> result = userService.getAll("john", "doe", pageable);

    assertThat(result.getContent()).containsExactly(expected);
    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  void updateAppliesChangesAndSaves() {
    UserDto request = request(EMAIL);
    User user = user();
    UserDto expected = response();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    when(userRepository.existsByEmailAndIdNot(EMAIL, USER_ID)).thenReturn(false);
    when(userRepository.saveAndFlush(user)).thenReturn(user);
    when(userMapper.toDto(user)).thenReturn(expected);

    UserDto result = userService.update(USER_ID, request);

    assertThat(result).isEqualTo(expected);
    verify(userMapper).updateEntity(request, user);
  }

  @Test
  void updateThrowsWhenUserMissing() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.update(USER_ID, request(EMAIL)))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void updateThrowsWhenEmailTakenByAnotherUser() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user()));
    when(userRepository.existsByEmailAndIdNot(EMAIL, USER_ID)).thenReturn(true);

    assertThatThrownBy(() -> userService.update(USER_ID, request(EMAIL)))
        .isInstanceOf(DuplicateResourceException.class);
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void activateSetsActiveTrue() {
    when(userRepository.updateActiveById(USER_ID, true)).thenReturn(1);

    userService.activate(USER_ID);

    verify(userRepository).updateActiveById(USER_ID, true);
  }

  @Test
  void activateThrowsWhenUserMissing() {
    when(userRepository.updateActiveById(anyLong(), anyBoolean())).thenReturn(0);

    assertThatThrownBy(() -> userService.activate(USER_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deactivateSetsActiveFalse() {
    when(userRepository.updateActiveById(USER_ID, false)).thenReturn(1);

    userService.deactivate(USER_ID);

    verify(userRepository).updateActiveById(USER_ID, false);
  }

  @Test
  void deactivateThrowsWhenUserMissing() {
    when(userRepository.updateActiveById(anyLong(), anyBoolean())).thenReturn(0);

    assertThatThrownBy(() -> userService.deactivate(USER_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void deleteRemovesExistingUser() {
    User user = user();
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

    userService.delete(USER_ID);

    verify(userRepository).delete(user);
  }

  @Test
  void deleteThrowsWhenUserMissing() {
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> userService.delete(USER_ID))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(userRepository, never()).delete(any(User.class));
  }

  private static UserDto request(String email) {
    return new UserDto(null, "John", "Doe", LocalDate.of(1990, 1, 1), email,
        null, null, null, null);
  }

  private static UserDto response() {
    return new UserDto(USER_ID, "John", "Doe", LocalDate.of(1990, 1, 1), EMAIL,
        true, List.of(), null, null);
  }

  private static User user() {
    User user = new User();
    user.setId(USER_ID);
    user.setName("John");
    user.setSurname("Doe");
    user.setBirthDate(LocalDate.of(1990, 1, 1));
    user.setEmail(EMAIL);
    return user;
  }
}
