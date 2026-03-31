package com.nexpath.controllers;

import com.nexpath.dtos.request.PaymentVerifyRequest;
import com.nexpath.dtos.request.PurchaseRequest;
import com.nexpath.dtos.response.ApiResponse;
import com.nexpath.dtos.response.CreditPackageResponse;
import com.nexpath.dtos.response.CreditTransactionResponse;
import com.nexpath.enums.CreditPackage;
import com.nexpath.exceptions.BadRequestException;
import com.nexpath.models.CreditTransaction;
import com.nexpath.models.User;
import com.nexpath.repository.UserRepository;
import com.nexpath.services.CreditService;
import com.nexpath.services.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;
    private final PaymentService paymentService;
    private final UserRepository userRepository;

    // ─────────────────────────────────────────
    // GET /api/credits/balance
    // ─────────────────────────────────────────
    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getBalance(@AuthenticationPrincipal Long userId) {
        User user = loadUser(userId);
        return ApiResponse.success("Credit balance", creditService.getBalance(user));
    }

    // ─────────────────────────────────────────
    // GET /api/credits/history
    // ─────────────────────────────────────────
    @GetMapping("/history")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CreditTransactionResponse> txPage = creditService.getHistory(userId, pageable);

        Map<String, Object> result = Map.of(
                "content", txPage.getContent(),
                "totalPages", txPage.getTotalPages(),
                "totalElements", txPage.getTotalElements(),
                "currentPage", txPage.getNumber(),
                "pageSize", txPage.getSize()
        );

        return ApiResponse.success("Credit history", result);
    }

    // ─────────────────────────────────────────
    // GET /api/credits/packages
    // ─────────────────────────────────────────
    @GetMapping("/packages")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> getPackages() {
        List<CreditPackageResponse> packages = Arrays.stream(CreditPackage.values())
                .map(pkg -> {
                    String priceDisplay = "₹" + (pkg.getAmountPaise() / 100);
                    return new CreditPackageResponse(
                            pkg.name(),
                            pkg.getDisplayName(),
                            pkg.getAmountPaise(),
                            pkg.getCredits(),
                            priceDisplay
                    );
                })
                .toList();

        return ApiResponse.success("Credit packages", packages);
    }

    // ─────────────────────────────────────────
    // POST /api/credits/purchase/order
    // ─────────────────────────────────────────
    @PostMapping("/purchase/order")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> createOrder(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PurchaseRequest request) {
        User user = loadUser(userId);
        return ApiResponse.success("Order created",
                paymentService.createOrder(user, request.getPackageName()));
    }

    // ─────────────────────────────────────────
    // POST /api/credits/purchase/verify
    // ─────────────────────────────────────────
    @PostMapping("/purchase/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<?> verifyPayment(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PaymentVerifyRequest request) {
        User user = loadUser(userId);
        paymentService.verifyPayment(
                user,
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
        return ApiResponse.success("Payment verified! Credits added to your account.", null);
    }

    // ─────────────────────────────────────────
    // Internal helper
    // ─────────────────────────────────────────
    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
}
