package com.example.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentCardDto(
    @JsonProperty(access = Access.READ_ONLY) Long id,
    @JsonProperty(access = Access.READ_ONLY) Long userId,
    @NotBlank @Pattern(regexp = "\\d{13,19}") String number,
    @NotBlank @Size(max = 200) String holder,
    @NotNull @FutureOrPresent LocalDate expirationDate,
    @JsonProperty(access = Access.READ_ONLY) Boolean active,
    @JsonProperty(access = Access.READ_ONLY) LocalDateTime createdAt,
    @JsonProperty(access = Access.READ_ONLY) LocalDateTime updatedAt) {}
