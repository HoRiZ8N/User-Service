package com.example.userservice.exception;

public class DuplicateResourceException extends RuntimeException {

  public DuplicateResourceException(String resource, String field) {
    super(resource + " with this " + field + " already exists");
  }
}
