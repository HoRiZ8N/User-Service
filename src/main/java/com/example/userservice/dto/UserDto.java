package com.example.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record UserDto(
    @JsonProperty(access = Access.READ_ONLY) Long id,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 100) String surname,
    @Past LocalDate birthDate,
    @NotBlank @Email @Size(max = 255) String email,
    @JsonProperty(access = Access.READ_ONLY) Boolean active,
    @JsonProperty(access = Access.READ_ONLY) List<PaymentCardDto> cards,
    @JsonProperty(access = Access.READ_ONLY) LocalDateTime createdAt,
    @JsonProperty(access = Access.READ_ONLY) LocalDateTime updatedAt) {}
