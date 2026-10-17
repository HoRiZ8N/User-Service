package com.example.userservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.userservice.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

  @Test
  void createPersistsUser() throws Exception {
    mockMvc.perform(post("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("John", "Doe", "john@example.com")))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/v1/users/1")))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.email").value("john@example.com"))
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.createdAt").isNotEmpty());

    assertThat(jdbcTemplate.queryForObject(
        "SELECT email FROM users WHERE id = 1", String.class)).isEqualTo("john@example.com");
  }

  @Test
  void createRejectsInvalidBody() throws Exception {
    mockMvc.perform(post("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("", "Doe", "not-an-email")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").exists())
        .andExpect(jsonPath("$.errors.email").exists());
  }

  @Test
  void createRejectsDuplicateEmail() throws Exception {
    createUser("John", "Doe", "john@example.com");

    mockMvc.perform(post("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("Jane", "Doe", "john@example.com")))
        .andExpect(status().isConflict());
  }

  @Test
  void getByIdReturnsUserWithCardsAndCachesIt() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    createCard(userId);

    mockMvc.perform(get("/api/v1/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(userId))
        .andExpect(jsonPath("$.cards", hasSize(1)));

    UserDto cached = usersCache().get(userId, UserDto.class);
    assertThat(cached).isNotNull();
    assertThat(cached.id()).isEqualTo(userId);
    assertThat(cached.cards()).hasSize(1);
  }

  @Test
  void getByIdServesCachedValue() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    mockMvc.perform(get("/api/v1/users/{id}", userId)).andExpect(status().isOk());

    jdbcTemplate.update("UPDATE users SET name = 'Changed' WHERE id = ?", userId);

    mockMvc.perform(get("/api/v1/users/{id}", userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("John"));
  }

  @Test
  void createAcceptsSpecialCharactersInFields() throws Exception {
    mockMvc.perform(post("/api/v1/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("Jean \"JJ\"", "O'Brien\\Smith", "jj@example.com")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Jean \"JJ\""))
        .andExpect(jsonPath("$.surname").value("O'Brien\\Smith"));
  }

  @Test
  void getByIdReturnsNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/users/{id}", 999))
        .andExpect(status().isNotFound());
  }

  @Test
  void getAllFiltersByNameAndSurname() throws Exception {
    createUser("John", "Doe", "john@example.com");
    createUser("Johnny", "Smith", "johnny@example.com");
    createUser("Jane", "Doe", "jane@example.com");

    mockMvc.perform(get("/api/v1/users").param("name", "john"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(2)))
        .andExpect(jsonPath("$.page.totalElements").value(2));

    mockMvc.perform(get("/api/v1/users").param("name", "john").param("surname", "doe"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].email").value("john@example.com"));
  }

  @Test
  void updateChangesUserAndRefreshesCache() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    mockMvc.perform(get("/api/v1/users/{id}", userId)).andExpect(status().isOk());

    mockMvc.perform(put("/api/v1/users/{id}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("Johnny", "Doe", "johnny@example.com")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Johnny"));

    assertThat(usersCache().get(userId, UserDto.class).name()).isEqualTo("Johnny");
    mockMvc.perform(get("/api/v1/users/{id}", userId))
        .andExpect(jsonPath("$.email").value("johnny@example.com"));
  }

  @Test
  void updateRejectsEmailOfAnotherUserAndKeepsCache() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    createUser("Jane", "Doe", "jane@example.com");
    mockMvc.perform(get("/api/v1/users/{id}", userId)).andExpect(status().isOk());

    mockMvc.perform(put("/api/v1/users/{id}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("John", "Doe", "jane@example.com")))
        .andExpect(status().isConflict());

    assertThat(usersCache().get(userId, UserDto.class).email()).isEqualTo("john@example.com");
  }

  @Test
  void updateRejectsInvalidBody() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");

    mockMvc.perform(put("/api/v1/users/{id}", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("", "Doe", "not-an-email")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.name").exists())
        .andExpect(jsonPath("$.errors.email").exists());
  }

  @Test
  void updateReturnsNotFound() throws Exception {
    mockMvc.perform(put("/api/v1/users/{id}", 999)
            .contentType(MediaType.APPLICATION_JSON)
            .content(userJson("John", "Doe", "john@example.com")))
        .andExpect(status().isNotFound());
  }

  @Test
  void deactivateAndActivateChangeStatusAndEvictCache() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    mockMvc.perform(get("/api/v1/users/{id}", userId)).andExpect(status().isOk());

    mockMvc.perform(patch("/api/v1/users/{id}/deactivate", userId))
        .andExpect(status().isNoContent());
    assertThat(usersCache().get(userId)).isNull();
    mockMvc.perform(get("/api/v1/users/{id}", userId))
        .andExpect(jsonPath("$.active").value(false));

    mockMvc.perform(patch("/api/v1/users/{id}/activate", userId))
        .andExpect(status().isNoContent());
    assertThat(usersCache().get(userId)).isNull();
    mockMvc.perform(get("/api/v1/users/{id}", userId))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void activateReturnsNotFound() throws Exception {
    mockMvc.perform(patch("/api/v1/users/{id}/activate", 999))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteRemovesUserWithCardsAndEvictsCache() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    createCard(userId);
    mockMvc.perform(get("/api/v1/users/{id}", userId)).andExpect(status().isOk());

    mockMvc.perform(delete("/api/v1/users/{id}", userId))
        .andExpect(status().isNoContent());

    assertThat(usersCache().get(userId)).isNull();
    assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM payment_cards WHERE user_id = ?", Long.class, userId)).isZero();
    mockMvc.perform(get("/api/v1/users/{id}", userId))
        .andExpect(status().isNotFound());
  }

  @Test
  void deleteReturnsNotFound() throws Exception {
    mockMvc.perform(delete("/api/v1/users/{id}", 999))
        .andExpect(status().isNotFound());
  }

  @Test
  void rejectsNonPositiveId() throws Exception {
    mockMvc.perform(get("/api/v1/users/{id}", 0))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.id").exists());
  }
}
