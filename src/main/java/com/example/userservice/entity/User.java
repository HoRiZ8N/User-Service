package com.example.userservice.entity;

import com.example.userservice.exception.CardLimitExceededException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User extends Auditable {

  public static final int MAX_CARDS = 5;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 100)
  private String surname;

  @Column(name = "birth_date")
  private LocalDate birthDate;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private boolean active = true;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PaymentCard> cards = new ArrayList<>();

  public void addCard(PaymentCard card) {
    if (cards.size() >= MAX_CARDS) {
      throw new CardLimitExceededException(id, MAX_CARDS);
    }
    cards.add(card);
    card.setUser(this);
  }

  public void removeCard(PaymentCard card) {
    cards.remove(card);
    card.setUser(null);
  }
}