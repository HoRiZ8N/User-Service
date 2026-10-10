package com.example.userservice.exception;

public class CardLimitExceededException extends RuntimeException {

  public CardLimitExceededException(Long userId, int limit) {
    super("User " + userId + " already has the maximum of " + limit + " cards");
  }
}
