package com.example.userservice.service;

import com.example.userservice.dto.PaymentCardDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentCardService {

  PaymentCardDto create(Long userId, PaymentCardDto dto);

  PaymentCardDto getById(Long id);

  Page<PaymentCardDto> getAll(String name, String surname, Pageable pageable);

  List<PaymentCardDto> getAllByUserId(Long userId);

  PaymentCardDto update(Long id, PaymentCardDto dto);

  void activate(Long id);

  void deactivate(Long id);
}
