package com.example.userservice.repository;

import com.example.userservice.entity.PaymentCard;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentCardRepository
    extends JpaRepository<PaymentCard, Long>, JpaSpecificationExecutor<PaymentCard> {

  List<PaymentCard> findAllByUserId(Long userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      value = "UPDATE payment_cards SET active = :active, updated_at = LOCALTIMESTAMP WHERE id = :id",
      nativeQuery = true)
  int updateActiveById(@Param("id") Long id, @Param("active") boolean active);
}
