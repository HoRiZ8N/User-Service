package com.example.userservice.service.impl;

import com.example.userservice.config.CacheNames;
import com.example.userservice.dto.PaymentCardDto;
import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.exception.CardLimitExceededException;
import com.example.userservice.exception.DuplicateResourceException;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.mapper.PaymentCardMapper;
import com.example.userservice.repository.PaymentCardRepository;
import com.example.userservice.repository.specification.PaymentCardSpecifications;
import com.example.userservice.service.PaymentCardService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentCardServiceImpl implements PaymentCardService {

  private static final int MAX_CARDS_PER_USER = 5;
  private static final String RESOURCE = "Payment card";
  private static final String USER_RESOURCE = "User";

  private final PaymentCardRepository paymentCardRepository;
  private final PaymentCardMapper paymentCardMapper;
  private final CacheManager cacheManager;

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheNames.USERS, key = "#userId")
  public PaymentCardDto create(Long userId, PaymentCardDto dto) {
    User user = paymentCardRepository.findUserByIdForUpdate(userId)
        .orElseThrow(() -> new ResourceNotFoundException(USER_RESOURCE, userId));
    if (paymentCardRepository.countByUserId(userId) >= MAX_CARDS_PER_USER) {
      throw new CardLimitExceededException(userId, MAX_CARDS_PER_USER);
    }
    if (paymentCardRepository.existsByNumber(dto.number())) {
      throw new DuplicateResourceException(RESOURCE, "number");
    }
    PaymentCard card = paymentCardMapper.toEntity(dto);
    user.addCard(card);
    return paymentCardMapper.toDto(paymentCardRepository.save(card));
  }

  @Override
  public PaymentCardDto getById(Long id) {
    return paymentCardMapper.toDto(findOrThrow(id));
  }

  @Override
  public Page<PaymentCardDto> getAll(String name, String surname, Pageable pageable) {
    return paymentCardRepository
        .findAll(PaymentCardSpecifications.byFilter(name, surname), pageable)
        .map(paymentCardMapper::toDto);
  }

  @Override
  public List<PaymentCardDto> getAllByUserId(Long userId) {
    if (!paymentCardRepository.existsUserById(userId)) {
      throw new ResourceNotFoundException(USER_RESOURCE, userId);
    }
    return paymentCardRepository.findAllByUserId(userId).stream()
        .map(paymentCardMapper::toDto)
        .toList();
  }

  @Override
  @Transactional
  @CacheEvict(cacheNames = CacheNames.USERS, key = "#result.userId")
  public PaymentCardDto update(Long id, PaymentCardDto dto) {
    PaymentCard card = findOrThrow(id);
    if (paymentCardRepository.existsByNumberAndIdNot(dto.number(), id)) {
      throw new DuplicateResourceException(RESOURCE, "number");
    }
    paymentCardMapper.updateEntity(dto, card);
    return paymentCardMapper.toDto(paymentCardRepository.saveAndFlush(card));
  }

  @Override
  @Transactional
  public void activate(Long id) {
    changeStatus(id, true);
  }

  @Override
  @Transactional
  public void deactivate(Long id) {
    changeStatus(id, false);
  }

  @Override
  public boolean isOwnedBy(Long id, Long userId) {
    return paymentCardRepository.existsByIdAndUserId(id, userId);
  }

  private PaymentCard findOrThrow(Long id) {
    return paymentCardRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
  }

  private void changeStatus(Long id, boolean active) {
    Long userId = paymentCardRepository.findUserIdById(id)
        .orElseThrow(() -> new ResourceNotFoundException(RESOURCE, id));
    paymentCardRepository.updateActiveById(id, active);
    evictUser(userId);
  }

  private void evictUser(Long userId) {
    Cache cache = cacheManager.getCache(CacheNames.USERS);
    if (cache != null) {
      cache.evict(userId);
    }
  }
}
