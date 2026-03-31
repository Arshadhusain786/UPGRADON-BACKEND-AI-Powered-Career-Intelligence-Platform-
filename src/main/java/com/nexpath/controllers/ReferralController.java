package com.nexpath.controllers;

import com.nexpath.dtos.response.ApiResponse;
import com.nexpath.models.User;
import com.nexpath.repository.UserRepository;
import com.nexpath.services.ReferralService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralService referralService;
    private final UserRepository userRepository;

    // ─────────────────────────────────────────
    // GET /api/referrals/stats
    // ─────────────────────────────────────────
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getStats(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ApiResponse.success("Referral stats", referralService.getStats(user));
    }

    // ─────────────────────────────────────────
    // GET /api/referrals/list
    // ─────────────────────────────────────────
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getList(
            @AuthenticationPrincipal Long userId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
            
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        org.springframework.data.domain.Page<com.nexpath.dtos.response.ReferralDto> referPage = referralService.getReferralsList(user, page, size);

        java.util.Map<String, Object> result = java.util.Map.of(
                "content", referPage.getContent(),
                "totalPages", referPage.getTotalPages(),
                "totalElements", referPage.getTotalElements(),
                "currentPage", referPage.getNumber(),
                "pageSize", referPage.getSize()
        );

        return ApiResponse.success("Referral list", result);
    }
}
