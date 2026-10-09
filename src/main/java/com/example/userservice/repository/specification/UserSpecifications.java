package com.example.userservice.repository.specification;

import com.example.userservice.entity.User;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class UserSpecifications {

  private UserSpecifications() {
  }

  public static Specification<User> hasName(String name) {
    return (root, query, cb) -> StringUtils.hasText(name)
        ? cb.like(cb.lower(root.get("name")), contains(name))
        : cb.conjunction();
  }

  public static Specification<User> hasSurname(String surname) {
    return (root, query, cb) -> StringUtils.hasText(surname)
        ? cb.like(cb.lower(root.get("surname")), contains(surname))
        : cb.conjunction();
  }

  public static Specification<User> byFilter(String name, String surname) {
    return Specification.allOf(hasName(name), hasSurname(surname));
  }

  private static String contains(String value) {
    return "%" + value.trim().toLowerCase() + "%";
  }
}
