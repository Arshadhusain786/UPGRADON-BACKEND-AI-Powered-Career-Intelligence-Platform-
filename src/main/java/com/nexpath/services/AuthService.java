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
    private final UserMapper userMapper;

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
                .build();

        user = userRepository.save(user);

        log.info("User registered: {} with role {}", user.getEmail(), user.getRole());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = createRefreshToken(user);

        return new AuthResponse(accessToken, refreshToken, userMapper.toDto(user));
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
                userMapper.toDto(user)
        );
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

        return userMapper.toDto(user);
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