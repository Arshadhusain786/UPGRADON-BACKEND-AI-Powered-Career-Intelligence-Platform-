package com.nexpath.controllers;

import com.nexpath.dtos.UserDto;
import com.nexpath.dtos.request.LoginRequest;
import com.nexpath.dtos.request.RefreshRequest;
import com.nexpath.dtos.request.RegisterRequest;
import com.nexpath.dtos.response.ApiResponse;
import com.nexpath.dtos.response.AuthResponse;
import com.nexpath.dtos.response.TokenResponse;
import com.nexpath.services.AuthService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 🔹 REGISTER (PUBLIC)
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    // 🔹 LOGIN (PUBLIC)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success("Login successful", response)
        );
    }

    // 🔹 REFRESH TOKEN (PUBLIC)
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(
            @Valid @RequestBody RefreshRequest request
    ) {
        TokenResponse response = authService.refreshToken(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed successfully", response)
        );
    }

    // 🔹 LOGOUT (AUTHENTICATED USERS ONLY)
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long userId
    ) {
        authService.logout(userId);

        return ResponseEntity.ok(
                ApiResponse.success("Logged out successfully", null)
        );
    }

    // 🔹 CURRENT USER (AUTHENTICATED)
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> getCurrentUser(
            @AuthenticationPrincipal Long userId
    ) {
        UserDto user = authService.getCurrentUser(userId);

        return ResponseEntity.ok(
                ApiResponse.success("User fetched successfully", user)
        );
    }

    // 🔹 UPDATE PROFILE (AUTHENTICATED)
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody com.nexpath.dtos.request.UpdateProfileRequest request
    ) {
        UserDto user = authService.updateProfile(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Profile updated successfully", user)
        );
    }

    // 🔹 CHANGE PASSWORD (AUTHENTICATED)
    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody com.nexpath.dtos.request.ChangePasswordRequest request
    ) {
        authService.changePassword(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Password updated successfully", null)
        );
    }

    // 🔥 ADMIN ONLY TEST ENDPOINT
    @GetMapping("/admin-test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> adminOnly() {
        return ResponseEntity.ok(
                ApiResponse.success("Admin access granted 🔥", "ADMIN_OK")
        );
    }

    // 🔥 MENTOR ONLY TEST ENDPOINT
    @GetMapping("/mentor-test")
    @PreAuthorize("hasRole('MENTOR')")
    public ResponseEntity<ApiResponse<String>> mentorOnly() {
        return ResponseEntity.ok(
                ApiResponse.success("Mentor access granted 🚀", "MENTOR_OK")
        );
    }

    // 🔥 ALL ROLES
    @GetMapping("/common-test")
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','ADMIN')")
    public ResponseEntity<ApiResponse<String>> commonAccess() {
        return ResponseEntity.ok(
                ApiResponse.success("Common access 👥", "COMMON_OK")
        );
    }
}