package com.example.userservice.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.userservice.config.CacheNames;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
abstract class AbstractIntegrationTest {

  private static final AtomicLong CARD_SEQUENCE = new AtomicLong(4000000000000000L);

  @Autowired
  protected MockMvc mockMvc;

  @Autowired
  protected JdbcTemplate jdbcTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private CacheManager cacheManager;

  @BeforeEach
  void cleanUp() {
    jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
    usersCache().invalidate();
  }

  protected Cache usersCache() {
    return Objects.requireNonNull(cacheManager.getCache(CacheNames.USERS));
  }

  protected Long createUser(String name, String surname, String email) throws Exception {
    String body = mockMvc.perform(post("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson(name, surname, email)))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return idOf(body);
  }

  protected Long createCard(Long userId) throws Exception {
    String body = mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(nextCardNumber(), "JOHN DOE")))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();
    return idOf(body);
  }

  protected String userJson(String name, String surname, String email) throws Exception {
    return objectMapper.writeValueAsString(Map.of(
        "name", name,
        "surname", surname,
        "birthDate", "1990-01-01",
        "email", email));
  }

  protected String cardJson(String number, String holder) throws Exception {
    return objectMapper.writeValueAsString(Map.of(
        "number", number,
        "holder", holder,
        "expirationDate", "2035-12-31"));
  }

  protected static String nextCardNumber() {
    return String.valueOf(CARD_SEQUENCE.incrementAndGet());
  }

  private static Long idOf(String json) {
    return ((Number) JsonPath.read(json, "$.id")).longValue();
  }
}
