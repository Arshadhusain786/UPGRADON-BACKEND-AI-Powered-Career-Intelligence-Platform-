package com.nexpath.services;

import com.nexpath.dtos.response.CreditBalanceResponse;
import com.nexpath.dtos.response.CreditTransactionResponse;
import com.nexpath.enums.CreditTransactionType;
import com.nexpath.exceptions.BadRequestException;
import com.nexpath.exceptions.InsufficientCreditsException;
import com.nexpath.models.CreditTransaction;
import com.nexpath.models.User;
import com.nexpath.models.UserCredits;
import com.nexpath.repository.CreditTransactionRepository;
import com.nexpath.repository.UserCreditsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import org.springframework.cache.annotation.Cacheable;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreditService {

    private final UserCreditsRepository userCreditsRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    // ─────────────────────────────────────────
    // Get or Create credits (solves old user missing credits issue)
    // ─────────────────────────────────────────
    // Get a user's credit data
    @Cacheable(value = "user_credits", key = "#user.id")
    public UserCredits getOrCreateCredits(User user) {
        return userCreditsRepository.findByUser(user)
                .orElseGet(() -> initializeCredits(user));
    }

    // ─────────────────────────────────────────
    // Initialize credits for a new user (signup bonus)
    // ─────────────────────────────────────────
    public UserCredits initializeCredits(User user) {
        UserCredits credits = UserCredits.builder()
                .user(user)
                .totalCredits(20)
                .freeTodayRemaining(10)
                .lastRefillDate(LocalDate.now())
                .totalEarned(20)
                .totalSpent(0)
                .lockedCredits(0)
                .freeConnectionsThisWeek(3)
                .lastWeeklyReset(LocalDate.now())
                .build();

        credits = userCreditsRepository.save(credits);

        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .type(CreditTransactionType.SIGNUP_BONUS)
                .amount(20)
                .description("Welcome bonus")
                .balanceAfter(20)
                .build();

        creditTransactionRepository.save(tx);

        log.info("Initialized 20 welcome credits for user: {}", user.getEmail());
        
        return credits;
    }

    // ─────────────────────────────────────────
    // Deduct credits before an AI call
    // ─────────────────────────────────────────
    // Deduct credits for an action (e.g., viewing standard contact, AI generation)
    @CacheEvict(value = "user_credits", key = "#user.id")
    public void deductCredits(User user, int amount, CreditTransactionType type, String description) {
        UserCredits credits = getOrCreateCredits(user);

        if (credits.getTotalCredits() < amount) {
            throw new InsufficientCreditsException();
        }

        credits.setTotalCredits(credits.getTotalCredits() - amount);
        credits.setTotalSpent(credits.getTotalSpent() + amount);
        userCreditsRepository.save(credits);

        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .type(type)
                .amount(-amount)
                .description(description)
                .balanceAfter(credits.getTotalCredits())
                .build();

        creditTransactionRepository.save(tx);
    }

    // ─────────────────────────────────────────
    // Add credits (purchase / referral / daily)
    // ─────────────────────────────────────────
    // Log a credit transaction (internal)
    @CacheEvict(value = "user_credits", key = "#user.id")
    public void addCredits(User user, int amount, CreditTransactionType type,
                           String description, String referenceId) {
        UserCredits credits = getOrCreateCredits(user);

        int currentBalance = (credits.getTotalCredits() != null) ? credits.getTotalCredits() : 0;
        int currentEarned = (credits.getTotalEarned() != null) ? credits.getTotalEarned() : 0;
        
        credits.setTotalCredits(currentBalance + amount);
        credits.setTotalEarned(currentEarned + amount);
        userCreditsRepository.save(credits);

        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .description(description)
                .referenceId(referenceId)
                .balanceAfter(credits.getTotalCredits())
                .build();

        creditTransactionRepository.save(tx);
    }

    // ─────────────────────────────────────────
    // Add free connections
    // ─────────────────────────────────────────
    @CacheEvict(value = "user_credits", key = "#user.id")
    public void addFreeConnections(User user, int count, String transactionRef) {
        UserCredits credits = getOrCreateCredits(user);
        int current = (credits.getFreeConnectionsThisWeek() != null) ? credits.getFreeConnectionsThisWeek() : 0;
        credits.setFreeConnectionsThisWeek(current + count);
        userCreditsRepository.save(credits);

        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .type(CreditTransactionType.PURCHASE)
                .amount(0) // 0 standard credits
                .description("In-App Purchase: +3 Connections Refill")
                .referenceId(transactionRef)
                .balanceAfter(credits.getTotalCredits())
                .build();

        creditTransactionRepository.save(tx);
    }

    // ─────────────────────────────────────────
    // Get current balance
    // ─────────────────────────────────────────
    @Cacheable(value = "user_credits", key = "#user.id")
    public CreditBalanceResponse getBalance(User user) {
        UserCredits credits = getOrCreateCredits(user);

        return new CreditBalanceResponse(
                (credits.getTotalCredits() != null) ? credits.getTotalCredits() : 0,
                (credits.getLockedCredits() != null) ? credits.getLockedCredits() : 0,
                (credits.getFreeTodayRemaining() != null) ? credits.getFreeTodayRemaining() : 0,
                (credits.getTotalEarned() != null) ? credits.getTotalEarned() : 0,
                (credits.getTotalSpent() != null) ? credits.getTotalSpent() : 0,
                (credits.getFreeConnectionsThisWeek() != null) ? credits.getFreeConnectionsThisWeek() : 0
        );
    }

    // ─────────────────────────────────────────
    // Log transaction without changing totalCredits (for locking/deduction logs)
    // ─────────────────────────────────────────
    public void logTransaction(User user, CreditTransactionType type, int amount,
                               String description, String referenceId) {
        UserCredits uc = getOrCreateCredits(user);
        CreditTransaction tx = CreditTransaction.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .description(description)
                .referenceId(referenceId)
                .balanceAfter(uc.getTotalCredits())
                .build();
        creditTransactionRepository.save(tx);
    }

    // ─────────────────────────────────────────
    // Paginated transaction history
    // ─────────────────────────────────────────
    public Page<CreditTransactionResponse> getHistory(Long userId, Pageable pageable) {
        return creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    private CreditTransactionResponse mapToResponse(CreditTransaction tx) {
        return CreditTransactionResponse.builder()
                .id(tx.getId())
                .type(tx.getType() != null ? tx.getType().name() : null)
                .amount(tx.getAmount())
                .description(tx.getDescription())
                .referenceId(tx.getReferenceId())
                .balanceAfter(tx.getBalanceAfter())
                .createdAt(tx.getCreatedAt())
                .build();
    }

    // ─────────────────────────────────────────
    // Scheduled daily free credit refill (midnight)
    // ─────────────────────────────────────────
    @Scheduled(cron = "0 0 0 * * *")
    public void refillDailyCredits() {
        LocalDate today = LocalDate.now();

        int pageSize = 500;
        int pageNumber = 0;
        org.springframework.data.domain.Page<UserCredits> pageResult;
        int totalProcessed = 0;

        do {
            pageResult = userCreditsRepository.findByLastRefillDateBeforeOrLastRefillDateIsNull(today, org.springframework.data.domain.PageRequest.of(pageNumber, pageSize));
            
            for (UserCredits uc : pageResult.getContent()) {
                uc.setFreeTodayRemaining(10);
                uc.setLastRefillDate(today);
                uc.setTotalCredits(uc.getTotalCredits() + 10);
                uc.setTotalEarned(uc.getTotalEarned() + 10);
                userCreditsRepository.save(uc);

                CreditTransaction tx = CreditTransaction.builder()
                        .user(uc.getUser())
                        .type(CreditTransactionType.EARNED_DAILY)
                        .amount(10)
                        .description("Daily free credits")
                        .balanceAfter(uc.getTotalCredits())
                        .build();

                creditTransactionRepository.save(tx);
                totalProcessed++;
            }
            pageNumber++;
        } while (pageResult.hasNext());

        log.info("Daily credit refill completed for {} users", totalProcessed);
    }
}
