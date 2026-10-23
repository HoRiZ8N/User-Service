package com.example.userservice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.userservice.security.JwtClaims;
import com.example.userservice.security.Role;
import com.example.userservice.security.SecurityErrorHandler;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class SecurityIntegrationTest extends AbstractIntegrationTest {

  @Test
  void rejectsRequestWithoutToken() throws Exception {
    anonymousMvc.perform(get("/api/v1/users/{id}", 1))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value(SecurityErrorHandler.AUTHENTICATION_REQUIRED))
        .andExpect(jsonPath("$.path").value("/api/v1/users/1"));
  }

  @Test
  void rejectsMalformedToken() throws Exception {
    assertInvalidToken("Bearer not-a-jwt");
  }

  @Test
  void rejectsTokenSignedWithUnknownKey() throws Exception {
    assertInvalidToken(TestJwt.foreign(Role.ADMIN, 1L));
  }

  @Test
  void rejectsExpiredToken() throws Exception {
    assertInvalidToken(TestJwt.bearer(claims -> claims
        .issuedAt(Instant.now().minusSeconds(3600))
        .expiresAt(Instant.now().minusSeconds(1800))));
  }

  @Test
  void rejectsRefreshToken() throws Exception {
    assertInvalidToken(TestJwt.bearer(claims -> claims.claim(JwtClaims.TYPE, "refresh")));
  }

  @Test
  void rejectsTokenWithUnknownRole() throws Exception {
    assertInvalidToken(TestJwt.bearer(claims -> claims.claim(JwtClaims.ROLE, "ROOT")));
  }

  @Test
  void rejectsUserTokenWithoutUserId() throws Exception {
    assertInvalidToken(TestJwt.bearer(claims -> claims
        .claims(c -> c.remove(JwtClaims.USER_ID))));
  }

  @Test
  void userReadsOwnProfile() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");

    mockMvc.perform(get("/api/v1/users/{id}", userId)
            .header(HttpHeaders.AUTHORIZATION, user(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId));
  }

  @Test
  void userReadsCurrentProfileFromToken() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");

    mockMvc.perform(get("/api/v1/users/me")
            .header(HttpHeaders.AUTHORIZATION, user(userId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.email").value("john@example.com"));
  }

  @Test
  void userUpdatesOwnProfile() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");

    mockMvc.perform(put("/api/v1/users/{id}", userId)
            .header(HttpHeaders.AUTHORIZATION, user(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("Johnny", "Doe", "john@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Johnny"));
  }

  @Test
  void userCannotAccessAnotherUser() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    Long otherId = createUser("Jane", "Roe", "jane@example.com");

    mockMvc.perform(get("/api/v1/users/{id}", otherId)
            .header(HttpHeaders.AUTHORIZATION, user(userId)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.message").value(SecurityErrorHandler.ACCESS_DENIED));
    mockMvc.perform(put("/api/v1/users/{id}", otherId)
            .header(HttpHeaders.AUTHORIZATION, user(userId))
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("Jane", "Roe", "jane@example.com")))
        .andExpect(status().isForbidden());
  }

  @Test
  void userCannotUseAdminEndpoints() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    String token = user(userId);

    mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/api/v1/users")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("Jane", "Roe", "jane@example.com")))
        .andExpect(status().isForbidden());
    mockMvc.perform(patch("/api/v1/users/{id}/deactivate", userId)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/v1/users/{id}", userId)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/v1/cards").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden());
  }

  @Test
  void userManagesOwnCards() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    String token = user(userId);

    mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(nextCardNumber(), "JOHN DOE")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(userId));
    Long cardId = createCard(userId);

    mockMvc.perform(get("/api/v1/users/{userId}/cards", userId)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/cards/{id}", cardId)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk());
    mockMvc.perform(put("/api/v1/cards/{id}", cardId)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(nextCardNumber(), "JOHN DOE")))
        .andExpect(status().isOk());
  }

  @Test
  void userCannotAccessAnotherUsersCards() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    Long otherId = createUser("Jane", "Roe", "jane@example.com");
    Long otherCardId = createCard(otherId);
    String token = user(userId);

    mockMvc.perform(post("/api/v1/users/{userId}/cards", otherId)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(nextCardNumber(), "JOHN DOE")))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/v1/users/{userId}/cards", otherId)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/v1/cards/{id}", otherCardId)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden());
    mockMvc.perform(put("/api/v1/cards/{id}", otherCardId)
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(nextCardNumber(), "JOHN DOE")))
        .andExpect(status().isForbidden());
    mockMvc.perform(patch("/api/v1/cards/{id}/deactivate", otherCardId)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden());
  }

  @Test
  void userGetsForbiddenForMissingCard() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");

    mockMvc.perform(get("/api/v1/cards/{id}", 999)
            .header(HttpHeaders.AUTHORIZATION, user(userId)))
        .andExpect(status().isForbidden());
  }

  @Test
  void serviceCreatesAndReadsUser() throws Exception {
    String token = TestJwt.service();
    String body = mockMvc.perform(post("/api/v1/users")
            .header(HttpHeaders.AUTHORIZATION, token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("John", "Doe", "john@example.com")))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    Long userId = idOf(body);

    mockMvc.perform(get("/api/v1/users/{id}", userId)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isForbidden());
  }

  private void assertInvalidToken(String token) throws Exception {
    anonymousMvc.perform(get("/api/v1/users/{id}", 1)
            .header(HttpHeaders.AUTHORIZATION, token))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\""))
        .andExpect(jsonPath("$.message").value(SecurityErrorHandler.INVALID_TOKEN));
  }

  private static String user(Long userId) {
    return TestJwt.bearer(Role.USER, userId);
  }
}
