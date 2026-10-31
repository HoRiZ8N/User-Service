package com.example.userservice.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

public enum Role {
  ADMIN,
  USER,
  SERVICE;

  public static Optional<Role> highestOf(Collection<String> names) {
    return Arrays.stream(values()).filter(role -> names.contains(role.name())).findFirst();
  }
}
