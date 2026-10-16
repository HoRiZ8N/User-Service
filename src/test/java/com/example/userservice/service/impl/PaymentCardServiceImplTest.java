package com.example.userservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.userservice.config.CacheNames;
import com.example.userservice.dto.PaymentCardDto;
import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import com.example.userservice.exception.CardLimitExceededException;
import com.example.userservice.exception.DuplicateResourceException;
import com.example.userservice.exception.ResourceNotFoundException;
import com.example.userservice.mapper.PaymentCardMapper;
import com.example.userservice.repository.PaymentCardRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class PaymentCardServiceImplTest {

  private static final Long USER_ID = 1L;
  private static final Long CARD_ID = 10L;
  private static final String NUMBER = "4111111111111111";

  @Mock
  private PaymentCardRepository paymentCardRepository;

  @Mock
  private PaymentCardMapper paymentCardMapper;

  @Mock
  private CacheManager cacheManager;

  @Mock
  private Cache cache;

  @InjectMocks
  private PaymentCardServiceImpl paymentCardService;

  @Test
  void createAddsCardToUser() {
    User user = user();
    PaymentCardDto request = request(NUMBER);
    PaymentCard card = card();
    PaymentCardDto expected = response();
    when(paymentCardRepository.findUserByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
    when(paymentCardRepository.countByUserId(USER_ID)).thenReturn(4L);
    when(paymentCardRepository.existsByNumber(NUMBER)).thenReturn(false);
    when(paymentCardMapper.toEntity(request)).thenReturn(card);
    when(paymentCardRepository.save(card)).thenReturn(card);
    when(paymentCardMapper.toDto(card)).thenReturn(expected);

    PaymentCardDto result = paymentCardService.create(USER_ID, request);

    assertThat(result).isEqualTo(expected);
    assertThat(user.getCards()).containsExactly(card);
    assertThat(card.getUser()).isSameAs(user);
  }

  @Test
  void createThrowsWhenUserMissing() {
    when(paymentCardRepository.findUserByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCardService.create(USER_ID, request(NUMBER)))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(paymentCardRepository, never()).save(any());
  }

  @Test
  void createThrowsWhenCardLimitReached() {
    when(paymentCardRepository.findUserByIdForUpdate(USER_ID)).thenReturn(Optional.of(user()));
    when(paymentCardRepository.countByUserId(USER_ID)).thenReturn(5L);

    assertThatThrownBy(() -> paymentCardService.create(USER_ID, request(NUMBER)))
        .isInstanceOf(CardLimitExceededException.class);
    verify(paymentCardRepository, never()).save(any());
  }

  @Test
  void createThrowsWhenNumberExists() {
    when(paymentCardRepository.findUserByIdForUpdate(USER_ID)).thenReturn(Optional.of(user()));
    when(paymentCardRepository.countByUserId(USER_ID)).thenReturn(0L);
    when(paymentCardRepository.existsByNumber(NUMBER)).thenReturn(true);

    assertThatThrownBy(() -> paymentCardService.create(USER_ID, request(NUMBER)))
        .isInstanceOf(DuplicateResourceException.class);
    verify(paymentCardRepository, never()).save(any());
  }

  @Test
  void getByIdReturnsCard() {
    PaymentCard card = card();
    PaymentCardDto expected = response();
    when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
    when(paymentCardMapper.toDto(card)).thenReturn(expected);

    assertThat(paymentCardService.getById(CARD_ID)).isEqualTo(expected);
  }

  @Test
  void getByIdThrowsWhenCardMissing() {
    when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCardService.getById(CARD_ID))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void getAllReturnsMappedPage() {
    Pageable pageable = PageRequest.of(0, 10);
    PaymentCard card = card();
    PaymentCardDto expected = response();
    when(paymentCardRepository.findAll(
        ArgumentMatchers.<Specification<PaymentCard>>any(), eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(card), pageable, 1));
    when(paymentCardMapper.toDto(card)).thenReturn(expected);

    Page<PaymentCardDto> result = paymentCardService.getAll("john", "doe", pageable);

    assertThat(result.getContent()).containsExactly(expected);
  }

  @Test
  void getAllByUserIdReturnsUserCards() {
    PaymentCard card = card();
    PaymentCardDto expected = response();
    when(paymentCardRepository.existsUserById(USER_ID)).thenReturn(true);
    when(paymentCardRepository.findAllByUserId(USER_ID)).thenReturn(List.of(card));
    when(paymentCardMapper.toDto(card)).thenReturn(expected);

    assertThat(paymentCardService.getAllByUserId(USER_ID)).containsExactly(expected);
  }

  @Test
  void getAllByUserIdThrowsWhenUserMissing() {
    when(paymentCardRepository.existsUserById(USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> paymentCardService.getAllByUserId(USER_ID))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(paymentCardRepository, never()).findAllByUserId(any());
  }

  @Test
  void updateAppliesChangesAndSaves() {
    PaymentCardDto request = request(NUMBER);
    PaymentCard card = card();
    PaymentCardDto expected = response();
    when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card));
    when(paymentCardRepository.existsByNumberAndIdNot(NUMBER, CARD_ID)).thenReturn(false);
    when(paymentCardRepository.saveAndFlush(card)).thenReturn(card);
    when(paymentCardMapper.toDto(card)).thenReturn(expected);

    PaymentCardDto result = paymentCardService.update(CARD_ID, request);

    assertThat(result).isEqualTo(expected);
    verify(paymentCardMapper).updateEntity(request, card);
  }

  @Test
  void updateThrowsWhenCardMissing() {
    when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCardService.update(CARD_ID, request(NUMBER)))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(paymentCardRepository, never()).saveAndFlush(any());
  }

  @Test
  void updateThrowsWhenNumberTakenByAnotherCard() {
    when(paymentCardRepository.findById(CARD_ID)).thenReturn(Optional.of(card()));
    when(paymentCardRepository.existsByNumberAndIdNot(NUMBER, CARD_ID)).thenReturn(true);

    assertThatThrownBy(() -> paymentCardService.update(CARD_ID, request(NUMBER)))
        .isInstanceOf(DuplicateResourceException.class);
    verify(paymentCardRepository, never()).saveAndFlush(any());
  }

  @Test
  void activateUpdatesStatusAndEvictsOwner() {
    when(paymentCardRepository.findUserIdById(CARD_ID)).thenReturn(Optional.of(USER_ID));
    when(cacheManager.getCache(CacheNames.USERS)).thenReturn(cache);

    paymentCardService.activate(CARD_ID);

    verify(paymentCardRepository).updateActiveById(CARD_ID, true);
    verify(cache).evict(USER_ID);
  }

  @Test
  void activateThrowsWhenCardMissing() {
    when(paymentCardRepository.findUserIdById(CARD_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCardService.activate(CARD_ID))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(paymentCardRepository, never()).updateActiveById(anyLong(), anyBoolean());
    verifyNoInteractions(cacheManager);
  }

  @Test
  void deactivateUpdatesStatusAndEvictsOwner() {
    when(paymentCardRepository.findUserIdById(CARD_ID)).thenReturn(Optional.of(USER_ID));
    when(cacheManager.getCache(CacheNames.USERS)).thenReturn(cache);

    paymentCardService.deactivate(CARD_ID);

    verify(paymentCardRepository).updateActiveById(CARD_ID, false);
    verify(cache).evict(USER_ID);
  }

  @Test
  void deactivateThrowsWhenCardMissing() {
    when(paymentCardRepository.findUserIdById(CARD_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> paymentCardService.deactivate(CARD_ID))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(paymentCardRepository, never()).updateActiveById(anyLong(), anyBoolean());
    verifyNoInteractions(cacheManager);
  }

  private static PaymentCardDto request(String number) {
    return new PaymentCardDto(null, null, number, "JOHN DOE", LocalDate.now().plusYears(2),
        null, null, null);
  }

  private static PaymentCardDto response() {
    return new PaymentCardDto(CARD_ID, USER_ID, NUMBER, "JOHN DOE",
        LocalDate.now().plusYears(2), true, null, null);
  }

  private static User user() {
    User user = new User();
    user.setId(USER_ID);
    return user;
  }

  private static PaymentCard card() {
    PaymentCard card = new PaymentCard();
    card.setId(CARD_ID);
    card.setNumber(NUMBER);
    card.setHolder("JOHN DOE");
    card.setExpirationDate(LocalDate.now().plusYears(2));
    return card;
  }
}
