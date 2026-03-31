package com.nexpath.services;

import com.nexpath.dtos.response.ReferralDto;
import com.nexpath.dtos.response.ReferralStatsResponse;
import com.nexpath.enums.CreditTransactionType;
import com.nexpath.models.Referral;
import com.nexpath.models.User;
import com.nexpath.repository.ReferralRepository;
import com.nexpath.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;

    // ─────────────────────────────────────────
    // Generate a unique referral code for a user
    // ─────────────────────────────────────────
    public String generateReferralCode(User user) {
        if (user.getReferralCode() != null) return user.getReferralCode();
        
        String code = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        user.setReferralCode(code);
        userRepository.save(user);
        return code;
    }

    // ─────────────────────────────────────────
    // Process a referral when a new user signs up
    // ─────────────────────────────────────────
    public void processReferral(User newUser, String referralCode) {
        if (referralCode == null || referralCode.isBlank()) return;

        userRepository.findByReferralCode(referralCode).ifPresent(referrer -> {
            // 1. Create Referral Record
            Referral referral = Referral.builder()
                    .referrer(referrer)
                    .referredUser(newUser)
                    .rewardCredits(20)
                    .build();
            referralRepository.save(referral);

            // 2. Award Referrer (20 credits)
            creditService.addCredits(
                    referrer,
                    20,
                    CreditTransactionType.REFERRAL,
                    "Referral bonus — invited " + newUser.getName(),
                    newUser.getId().toString()
            );

            // 3. Award Referred User (70 credits)
            creditService.addCredits(
                    newUser,
                    70,
                    CreditTransactionType.REFERRAL,
                    "Referral bonus — joined via " + referrer.getName(),
                    referrer.getId().toString()
            );

            log.info("Referral processed: {} referred {}", referrer.getEmail(), newUser.getEmail());
        });
    }

    // ─────────────────────────────────────────
    // Get Referral Stats for a user
    // ─────────────────────────────────────────
    public ReferralStatsResponse getStats(User user) {
        String code = generateReferralCode(user);
        long count = referralRepository.countByReferrer(user);
        Integer totalEarned = referralRepository.sumRewardCreditsByReferrer(user);
        
        // Base dynamic URL (ideally from Config/Environment)
        String baseUrl = "https://upgradon.com"; // Default to production
        
        List<ReferralDto> referrals = referralRepository.findByReferrerOrderByCreatedAtDesc(user)
                .stream()
                .map(r -> ReferralDto.builder()
                        .name(r.getReferredUser().getName())
                        .email(maskEmail(r.getReferredUser().getEmail()))
                        .rewardCredits(r.getRewardCredits())
                        .joinedAt(r.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ReferralStatsResponse.builder()
                .referralCode(code)
                .referralLink(baseUrl + "/register?ref=" + code)
                .totalReferrals(count)
                .totalEarned(totalEarned != null ? totalEarned : 0)
                .referrals(referrals)
                .build();
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
    
    public org.springframework.data.domain.Page<ReferralDto> getReferralsList(User user, int page, int size) {
        return referralRepository.findByReferrerOrderByCreatedAtDesc(user, org.springframework.data.domain.PageRequest.of(page, size))
                .map(r -> ReferralDto.builder()
                        .name(r.getReferredUser().getName())
                        .email(maskEmail(r.getReferredUser().getEmail()))
                        .rewardCredits(r.getRewardCredits())
                        .joinedAt(r.getCreatedAt())
                        .build());
    }
}
