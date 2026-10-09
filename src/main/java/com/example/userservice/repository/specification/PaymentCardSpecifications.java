package com.example.userservice.repository.specification;

import com.example.userservice.entity.PaymentCard;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class PaymentCardSpecifications {

  private PaymentCardSpecifications() {
  }

  public static Specification<PaymentCard> hasUserName(String name) {
    return (root, query, cb) -> StringUtils.hasText(name)
        ? cb.like(cb.lower(root.get("user").get("name")), contains(name))
        : cb.conjunction();
  }

  public static Specification<PaymentCard> hasUserSurname(String surname) {
    return (root, query, cb) -> StringUtils.hasText(surname)
        ? cb.like(cb.lower(root.get("user").get("surname")), contains(surname))
        : cb.conjunction();
  }

  public static Specification<PaymentCard> byFilter(String name, String surname) {
    return Specification.allOf(hasUserName(name), hasUserSurname(surname));
  }

  private static String contains(String value) {
    return "%" + value.trim().toLowerCase() + "%";
  }
}
