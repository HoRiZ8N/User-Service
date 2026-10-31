package com.example.userservice.security;

import com.example.userservice.service.PaymentCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("access")
@RequiredArgsConstructor
public class AccessGuard {

  private final PaymentCardService paymentCardService;

  public boolean isCurrentUser(Long userId) {
    Long currentUserId = currentUserId();
    return currentUserId != null && currentUserId.equals(userId);
  }

  public boolean ownsCard(Long cardId) {
    Long currentUserId = currentUserId();
    return currentUserId != null && paymentCardService.isOwnedBy(cardId, currentUserId);
  }

  private Long currentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication instanceof JwtAuthenticationToken token
        ? JwtClaims.userId(token.getToken())
        : null;
  }
}
