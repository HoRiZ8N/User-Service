package com.example.userservice.repository;

import com.example.userservice.entity.PaymentCard;
import com.example.userservice.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCardRepository
    extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {

  List<PaymentCard> findAllByUserId(Long userId);

  long countByUserId(Long userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :userId")
  Optional<User> findUserByIdForUpdate(@Param("userId") Long userId);

  @Query("select count(u) > 0 from User u where u.id = :userId")
  boolean existsUserById(@Param("userId") Long userId);

  boolean existsByNumber(String number);

  boolean existsByNumberAndIdNot(String number, Long id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value = "UPDATE payment_cards SET active = :active, updated_at = LOCALTIMESTAMP WHERE id = :id",
      nativeQuery = true)
  int updateActiveById(@Param("id") Long id, @Param("active") boolean active);
}
