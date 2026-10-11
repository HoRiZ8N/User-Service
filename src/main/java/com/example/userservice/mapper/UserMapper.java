package com.example.userservice.mapper;

import com.example.userservice.dto.UserDto;
import com.example.userservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(uses = PaymentCardMapper.class)
public interface UserMapper {

  UserDto toDto(User user);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "cards", ignore = true)
  User toEntity(UserDto dto);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "active", ignore = true)
  @Mapping(target = "cards", ignore = true)
  void updateEntity(UserDto dto, @MappingTarget User user);
}
