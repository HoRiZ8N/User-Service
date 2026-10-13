package com.example.userservice.controller;

import com.example.userservice.dto.PaymentCardDto;
import com.example.userservice.service.PaymentCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentCardController {

  private final PaymentCardService paymentCardService;

  @PostMapping("/users/{userId}/cards")
  public ResponseEntity<PaymentCardDto> create(
      @PathVariable @Positive Long userId, @Valid @RequestBody PaymentCardDto dto) {
    PaymentCardDto created = paymentCardService.create(userId, dto);
    URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
        .path("/api/v1/cards/{id}")
        .buildAndExpand(created.id())
        .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @GetMapping("/users/{userId}/cards")
  public ResponseEntity<List<PaymentCardDto>> getAllByUserId(@PathVariable @Positive Long userId) {
    return ResponseEntity.ok(paymentCardService.getAllByUserId(userId));
  }

  @GetMapping("/cards/{id}")
  public ResponseEntity<PaymentCardDto> getById(@PathVariable @Positive Long id) {
    return ResponseEntity.ok(paymentCardService.getById(id));
  }

  @GetMapping("/cards")
  public ResponseEntity<PagedModel<PaymentCardDto>> getAll(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String surname,
      @PageableDefault(sort = "id") Pageable pageable) {
    return ResponseEntity.ok(
        new PagedModel<>(paymentCardService.getAll(name, surname, pageable)));
  }

  @PutMapping("/cards/{id}")
  public ResponseEntity<PaymentCardDto> update(
      @PathVariable @Positive Long id, @Valid @RequestBody PaymentCardDto dto) {
    return ResponseEntity.ok(paymentCardService.update(id, dto));
  }

  @PatchMapping("/cards/{id}/activate")
  public ResponseEntity<Void> activate(@PathVariable @Positive Long id) {
    paymentCardService.activate(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/cards/{id}/deactivate")
  public ResponseEntity<Void> deactivate(@PathVariable @Positive Long id) {
    paymentCardService.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
