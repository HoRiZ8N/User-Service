package com.example.userservice.security;

import java.util.List;
import java.util.Map;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtClaims {

  public static final String USER_ID = "userId";
  public static final String REALM_ACCESS = "realm_access";
  public static final String ROLES = "roles";
  public static final String TYPE = "typ";
  public static final String ACCESS_TYPE = "Bearer";

  private JwtClaims() {
  }

  public static List<String> realmRoles(Jwt jwt) {
    if (jwt.getClaim(REALM_ACCESS) instanceof Map<?, ?> access
        && access.get(ROLES) instanceof List<?> roles) {
      return roles.stream().map(String::valueOf).toList();
    }
    return List.of();
  }

  public static Long userId(Jwt jwt) {
    return jwt.getClaim(USER_ID) instanceof Number id ? id.longValue() : null;
  }
}
