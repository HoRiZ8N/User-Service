package com.example.userservice.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AccessTokenValidator implements OAuth2TokenValidator<Jwt> {

  @Override
  public OAuth2TokenValidatorResult validate(Jwt jwt) {
    if (jwt.getExpiresAt() == null) {
      return failure("Token has no expiration");
    }
    if (!JwtClaims.ACCESS_TYPE.equals(jwt.getClaimAsString(JwtClaims.TYPE))) {
      return failure("Token is not an access token");
    }
    Role role = Role.highestOf(JwtClaims.realmRoles(jwt)).orElse(null);
    if (role == null) {
      return failure("Token has no valid role");
    }
    if (role != Role.SERVICE && JwtClaims.userId(jwt) == null) {
      return failure("Token has no user id");
    }
    return OAuth2TokenValidatorResult.success();
  }

  private static OAuth2TokenValidatorResult failure(String description) {
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, description, null));
  }
}
