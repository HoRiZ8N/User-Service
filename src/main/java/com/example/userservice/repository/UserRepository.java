package com.example.userservice.repository;

import com.example.userservice.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

  @Query("select u from User u left join fetch u.cards where u.id = :id")
  Optional<User> findByIdWithCards(@Param("id") Long id);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update User u set u.active = :active, u.updatedAt = local datetime where u.id = :id")
  int updateActiveById(@Param("id") Long id, @Param("active") boolean active);
}
