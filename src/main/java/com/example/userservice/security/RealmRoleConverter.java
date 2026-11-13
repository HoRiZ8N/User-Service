package com.example.userservice.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class RealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    List<String> roles = JwtClaims.realmRoles(jwt);
    return Arrays.stream(Role.values())
        .filter(role -> roles.contains(role.name()))
        .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
        .toList();
  }
}
