package com.example.userservice.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@TestConfiguration(proxyBeanMethods = false)
class TestJwtDecoderConfig {

  @Bean
  @Primary
  JwtDecoder testJwtDecoder(OAuth2TokenValidator<Jwt> jwtValidator) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(TestJwt.publicKey())
        .signatureAlgorithm(SignatureAlgorithm.RS256)
        .build();
    decoder.setJwtValidator(jwtValidator);
    return decoder;
  }
}
