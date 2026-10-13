package com.example.userservice.controller;

import com.example.userservice.dto.UserDto;
import com.example.userservice.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.net.URI;
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
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserDto> create(@Valid @RequestBody UserDto dto) {
    UserDto created = userService.create(dto);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{id}")
        .buildAndExpand(created.id())
        .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @GetMapping("/{id}")
  public ResponseEntity<UserDto> getById(@PathVariable @Positive Long id) {
    return ResponseEntity.ok(userService.getById(id));
  }

  @GetMapping
  public ResponseEntity<PagedModel<UserDto>> getAll(
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String surname,
      @PageableDefault(sort = "id") Pageable pageable) {
    return ResponseEntity.ok(new PagedModel<>(userService.getAll(name, surname, pageable)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<UserDto> update(
      @PathVariable @Positive Long id, @Valid @RequestBody UserDto dto) {
    return ResponseEntity.ok(userService.update(id, dto));
  }

  @PatchMapping("/{id}/activate")
  public ResponseEntity<Void> activate(@PathVariable @Positive Long id) {
    userService.activate(id);
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivate(@PathVariable @Positive Long id) {
    userService.deactivate(id);
    return ResponseEntity.noContent().build();
  }
}
