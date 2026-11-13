package com.example.userservice.security;

import com.example.userservice.dto.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

  public static final String AUTHENTICATION_REQUIRED = "Authentication is required";
  public static final String INVALID_TOKEN = "Access token is invalid or expired";
  public static final String ACCESS_DENIED = "Access is denied";

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    boolean invalidToken = ex instanceof InvalidBearerTokenException;
    response.setHeader(HttpHeaders.WWW_AUTHENTICATE,
        invalidToken ? "Bearer error=\"invalid_token\"" : "Bearer");
    write(response, HttpStatus.UNAUTHORIZED,
        invalidToken ? INVALID_TOKEN : AUTHENTICATION_REQUIRED, request);
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    write(response, HttpStatus.FORBIDDEN, ACCESS_DENIED, request);
  }

  private void write(
      HttpServletResponse response, HttpStatus status, String message, HttpServletRequest request)
      throws IOException {
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(),
        ErrorResponseDto.of(status, message, request.getRequestURI(), Map.of()));
  }
}
