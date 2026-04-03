package com.nexpath.repository;

import com.nexpath.models.User;
import com.nexpath.models.UserCredits;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCreditsRepository extends JpaRepository<UserCredits, Long> {

    Optional<UserCredits> findByUserId(Long userId);

    Optional<UserCredits> findByUser(User user);

    org.springframework.data.domain.Page<UserCredits> findByLastRefillDateBeforeOrLastRefillDateIsNull(java.time.LocalDate date, org.springframework.data.domain.Pageable pageable);
}
