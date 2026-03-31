package com.nexpath.services;

import com.nexpath.dtos.response.CreditBalanceResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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
    private UserCredits getOrCreateCredits(User user) {
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
    public void addCredits(User user, int amount, CreditTransactionType type,
                           String description, String referenceId) {
        UserCredits credits = getOrCreateCredits(user);

        credits.setTotalCredits(credits.getTotalCredits() + amount);
        credits.setTotalEarned(credits.getTotalEarned() + amount);
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
    // Get current balance
    // ─────────────────────────────────────────
    public CreditBalanceResponse getBalance(User user) {
        UserCredits credits = getOrCreateCredits(user);

        return new CreditBalanceResponse(
                credits.getTotalCredits(),
                credits.getFreeTodayRemaining(),
                credits.getTotalEarned(),
                credits.getTotalSpent()
        );
    }

    // ─────────────────────────────────────────
    // Paginated transaction history
    // ─────────────────────────────────────────
    public Page<CreditTransaction> getHistory(Long userId, Pageable pageable) {
        return creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    // ─────────────────────────────────────────
    // Scheduled daily free credit refill (midnight)
    // ─────────────────────────────────────────
    @Scheduled(cron = "0 0 0 * * *")
    public void refillDailyCredits() {
        LocalDate today = LocalDate.now();

        List<UserCredits> staleAccounts = userCreditsRepository.findAll()
                .stream()
                .filter(uc -> uc.getLastRefillDate() == null || uc.getLastRefillDate().isBefore(today))
                .toList();

        for (UserCredits uc : staleAccounts) {
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
        }

        log.info("Daily credit refill completed for {} users", staleAccounts.size());
    }
}
