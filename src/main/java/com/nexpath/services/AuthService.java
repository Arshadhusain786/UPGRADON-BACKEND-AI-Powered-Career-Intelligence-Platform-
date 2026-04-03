package com.nexpath.services;

import com.nexpath.dtos.UserDto;
import com.nexpath.dtos.request.LoginRequest;
import com.nexpath.dtos.request.RegisterRequest;
import com.nexpath.dtos.response.AuthResponse;
import com.nexpath.dtos.response.TokenResponse;
import com.nexpath.enums.UserRole;
import com.nexpath.exceptions.BadRequestException;
import com.nexpath.exceptions.UnauthorizedException;
import com.nexpath.mapper.UserMapper;
import com.nexpath.models.RefreshToken;
import com.nexpath.models.User;
import com.nexpath.repository.RefreshTokenRepository;
import com.nexpath.repository.UserRepository;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final CreditService creditService;
    private final ReferralService referralService;

    // 🔹 REGISTER
    @Transactional
    public AuthResponse register(@Valid RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        // ✅ SAFE ROLE HANDLING
        UserRole role;

        if (request.getRole() == null || request.getRole().isBlank()) {
            role = UserRole.STUDENT;
        } else {
            try {
                role = UserRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid role. Allowed: STUDENT, MENTOR, COMPANY, ADMIN");
            }
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .emailVerified(false)
                .referredByCode(request.getReferralCode()) // Track source
                .build();

        user = userRepository.save(user);

        // 💎 Step 1: ALWAYS award Signup Bonus (20 credits)
        creditService.initializeCredits(user);

        // 🤝 Step 2: Handle Referral (70 for new user, 20 for referrer)
        if (request.getReferralCode() != null && !request.getReferralCode().isBlank()) {
            referralService.processReferral(user, request.getReferralCode());
        }

        // 🔑 Step 3: Generate this user's unique sharing code
        referralService.generateReferralCode(user);

        log.info("User registered & credits initialized: {}", user.getEmail());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, UserMapper.toDto(user));
    }

    // 🔹 LOGIN
    @Transactional
    public AuthResponse login(@Valid LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .filter(User::isActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        log.info("User logged in: {}", user.getEmail());

        return new AuthResponse(
                accessToken,
                refreshToken,
                UserMapper.toDto(user));
    }

    // 🔹 REFRESH TOKEN
    @Transactional
    public TokenResponse refreshToken(String refreshTokenStr) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = createRefreshToken(user);

        return new TokenResponse(newAccessToken, newRefreshToken);
    }

    // 🔹 LOGOUT
    @Transactional
    public void logout(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        refreshTokenRepository.deleteByUser(user);

        log.info("User logged out: {}", user.getEmail());
    }

    // 🔹 CURRENT USER
    public UserDto getCurrentUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return UserMapper.toDto(user);
    }

    // 🔹 UPDATE PROFILE
    @Transactional
    public UserDto updateProfile(Long userId, @Valid com.nexpath.dtos.request.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Check if email is already taken by another user
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already in use");
        }

        user.setName(request.getName());
        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getTargetRole() != null) user.setTargetRole(request.getTargetRole());
        if (request.getSkills() != null) user.setSkills(request.getSkills());
        if (request.getTheme() != null) user.setTheme(request.getTheme());

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());

        return UserMapper.toDto(user);
    }

    // 🔐 CHANGE PASSWORD
    @Transactional
    public void changePassword(Long userId, com.nexpath.dtos.request.ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        //information

        log.info("Password updated for user: {}", user.getEmail());
    }

    // 🔐 CREATE REFRESH TOKEN
    private String createRefreshToken(User user) {

        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(LocalDateTime.now().plusDays(7));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);

        return refreshToken.getToken();
    }
}