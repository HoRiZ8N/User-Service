package com.example.userservice.integration;

import com.example.userservice.security.JwtClaims;
import com.example.userservice.security.Role;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.function.Consumer;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

final class TestJwt {

  private static final KeyPair KEY_PAIR = generateKeyPair();
  private static final JwtEncoder ENCODER = encoder(KEY_PAIR);
  private static final JwtEncoder FOREIGN_ENCODER = encoder(generateKeyPair());

  private TestJwt() {
  }

  static String publicKeyPem() {
    String body = Base64.getMimeEncoder(64, "\n".getBytes())
        .encodeToString(KEY_PAIR.getPublic().getEncoded());
    return "-----BEGIN PUBLIC KEY-----\n" + body + "\n-----END PUBLIC KEY-----\n";
  }

  static String bearer(Role role, Long userId) {
    return bearer(ENCODER, claims -> claims
        .claim(JwtClaims.ROLE, role.name())
        .claim(JwtClaims.USER_ID, userId));
  }

  static String service() {
    return bearer(ENCODER, claims -> claims.claim(JwtClaims.ROLE, Role.SERVICE.name()));
  }

  static String bearer(Consumer<JwtClaimsSet.Builder> customizer) {
    return bearer(ENCODER, claims -> {
      claims.claim(JwtClaims.ROLE, Role.USER.name()).claim(JwtClaims.USER_ID, 1L);
      customizer.accept(claims);
    });
  }

  static String foreign(Role role, Long userId) {
    return bearer(FOREIGN_ENCODER, claims -> claims
        .claim(JwtClaims.ROLE, role.name())
        .claim(JwtClaims.USER_ID, userId));
  }

  private static String bearer(JwtEncoder encoder, Consumer<JwtClaimsSet.Builder> customizer) {
    Instant now = Instant.now();
    JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
        .subject("test")
        .issuedAt(now)
        .expiresAt(now.plus(15, ChronoUnit.MINUTES))
        .claim(JwtClaims.TYPE, JwtClaims.ACCESS_TYPE);
    customizer.accept(claims);
    JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
    return "Bearer " + encoder.encode(JwtEncoderParameters.from(header, claims.build()))
        .getTokenValue();
  }

  private static JwtEncoder encoder(KeyPair keyPair) {
    RSAKey key = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
        .privateKey((RSAPrivateKey) keyPair.getPrivate())
        .build();
    return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
  }

  private static KeyPair generateKeyPair() {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(2048);
      return generator.generateKeyPair();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
