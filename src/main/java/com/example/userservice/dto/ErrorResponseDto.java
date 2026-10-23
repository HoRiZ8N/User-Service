package com.example.userservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.http.HttpStatus;

public record ErrorResponseDto(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path,
    @JsonInclude(Include.NON_EMPTY) Map<String, String> errors) {

  public static ErrorResponseDto of(
      HttpStatus status, String message, String path, Map<String, String> errors) {
    return new ErrorResponseDto(
        LocalDateTime.now(), status.value(), status.getReasonPhrase(), message, path, errors);
  }
}
