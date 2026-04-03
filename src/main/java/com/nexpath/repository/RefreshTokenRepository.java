package com.nexpath.repository;

import com.nexpath.models.RefreshToken;
import com.nexpath.models.User;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Transactional
    void deleteByUser(User user);

    @Transactional
    void deleteAllByExpiryDateBefore(LocalDateTime expiryDate);
}