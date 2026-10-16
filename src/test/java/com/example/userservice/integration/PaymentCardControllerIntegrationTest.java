package com.example.userservice.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PaymentCardControllerIntegrationTest extends AbstractIntegrationTest {

  @Test
  void createPersistsCardForUser() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    String number = nextCardNumber();

    mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(number, "JOHN DOE")))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", endsWith("/api/v1/cards/1")))
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.number").value(number))
        .andExpect(jsonPath("$.active").value(true));

    assertThat(jdbcTemplate.queryForObject(
        "SELECT user_id FROM payment_cards WHERE number = ?", Long.class, number))
        .isEqualTo(userId);
  }

  @Test
  void createEvictsOwnerFromCache() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    mockMvc.perform(get("/api/v1/users/{id}", userId))
        .andExpect(jsonPath("$.cards", hasSize(0)));

    createCard(userId);

    assertThat(usersCache().get(userId)).isNull();
    mockMvc.perform(get("/api/v1/users/{id}", userId))
        .andExpect(jsonPath("$.cards", hasSize(1)));
  }

  @Test
  void createReturnsNotFoundForMissingUser() throws Exception {
    mockMvc.perform(post("/api/v1/users/{userId}/cards", 999)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(nextCardNumber(), "JOHN DOE")))
        .andExpect(status().isNotFound());
  }

  @Test
  void createRejectsSixthCard() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    for (int i = 0; i < 5; i++) {
      createCard(userId);
    }

    mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(nextCardNumber(), "JOHN DOE")))
        .andExpect(status().isConflict());

    assertThat(jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM payment_cards WHERE user_id = ?", Long.class, userId))
        .isEqualTo(5);
  }

  @Test
  void createRejectsDuplicateNumber() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    String number = nextCardNumber();
    mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(number, "JOHN DOE")))
        .andExpect(status().isCreated());

    mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(number, "JOHN DOE")))
        .andExpect(status().isConflict());
  }

  @Test
  void createRejectsInvalidBody() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");

    mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson("12ab", "")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.number").exists())
        .andExpect(jsonPath("$.errors.holder").exists());
  }

  @Test
  void getByIdReturnsCard() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    Long cardId = createCard(userId);

    mockMvc.perform(get("/api/v1/cards/{id}", cardId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(cardId))
        .andExpect(jsonPath("$.userId").value(userId));
  }

  @Test
  void getByIdReturnsNotFound() throws Exception {
    mockMvc.perform(get("/api/v1/cards/{id}", 999))
        .andExpect(status().isNotFound());
  }

  @Test
  void getAllByUserIdReturnsOnlyUserCards() throws Exception {
    Long johnId = createUser("John", "Doe", "john@example.com");
    Long janeId = createUser("Jane", "Doe", "jane@example.com");
    createCard(johnId);
    createCard(johnId);
    createCard(janeId);

    mockMvc.perform(get("/api/v1/users/{userId}/cards", johnId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(2)));
  }

  @Test
  void getAllByUserIdReturnsNotFoundForMissingUser() throws Exception {
    mockMvc.perform(get("/api/v1/users/{userId}/cards", 999))
        .andExpect(status().isNotFound());
  }

  @Test
  void getAllFiltersByOwnerNameAndSurname() throws Exception {
    Long johnId = createUser("John", "Doe", "john@example.com");
    Long janeId = createUser("Jane", "Smith", "jane@example.com");
    createCard(johnId);
    createCard(janeId);

    mockMvc.perform(get("/api/v1/cards").param("surname", "smi"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content", hasSize(1)))
        .andExpect(jsonPath("$.content[0].userId").value(janeId));

    mockMvc.perform(get("/api/v1/cards"))
        .andExpect(jsonPath("$.page.totalElements").value(2));
  }

  @Test
  void updateChangesCardAndEvictsOwner() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    Long cardId = createCard(userId);
    mockMvc.perform(get("/api/v1/users/{id}", userId)).andExpect(status().isOk());
    String newNumber = nextCardNumber();

    mockMvc.perform(put("/api/v1/cards/{id}", cardId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(newNumber, "J DOE")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.number").value(newNumber))
        .andExpect(jsonPath("$.holder").value("J DOE"));

    assertThat(usersCache().get(userId)).isNull();
    mockMvc.perform(get("/api/v1/users/{id}", userId))
        .andExpect(jsonPath("$.cards[0].number").value(newNumber));
  }

  @Test
  void updateRejectsNumberOfAnotherCard() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    Long cardId = createCard(userId);
    String takenNumber = nextCardNumber();
    mockMvc.perform(post("/api/v1/users/{userId}/cards", userId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(takenNumber, "JOHN DOE")))
        .andExpect(status().isCreated());

    mockMvc.perform(put("/api/v1/cards/{id}", cardId)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(takenNumber, "JOHN DOE")))
        .andExpect(status().isConflict());
  }

  @Test
  void updateReturnsNotFound() throws Exception {
    mockMvc.perform(put("/api/v1/cards/{id}", 999)
            .contentType(MediaType.APPLICATION_JSON)
            .content(cardJson(nextCardNumber(), "JOHN DOE")))
        .andExpect(status().isNotFound());
  }

  @Test
  void deactivateAndActivateChangeStatusAndEvictOwner() throws Exception {
    Long userId = createUser("John", "Doe", "john@example.com");
    Long cardId = createCard(userId);
    mockMvc.perform(get("/api/v1/users/{id}", userId)).andExpect(status().isOk());

    mockMvc.perform(patch("/api/v1/cards/{id}/deactivate", cardId))
        .andExpect(status().isNoContent());
    assertThat(usersCache().get(userId)).isNull();
    mockMvc.perform(get("/api/v1/users/{id}", userId))
        .andExpect(jsonPath("$.cards[0].active").value(false));

    mockMvc.perform(patch("/api/v1/cards/{id}/activate", cardId))
        .andExpect(status().isNoContent());
    assertThat(usersCache().get(userId)).isNull();
    mockMvc.perform(get("/api/v1/cards/{id}", cardId))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  void deactivateReturnsNotFound() throws Exception {
    mockMvc.perform(patch("/api/v1/cards/{id}/deactivate", 999))
        .andExpect(status().isNotFound());
  }
}
