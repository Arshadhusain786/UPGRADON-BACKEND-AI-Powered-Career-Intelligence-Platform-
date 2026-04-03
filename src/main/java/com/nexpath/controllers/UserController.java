package com.nexpath.controllers;

import com.nexpath.dtos.request.UpdateProfileRequest;
import com.nexpath.dtos.response.ApiResponse;
import com.nexpath.dtos.response.UserProfileDto;
import com.nexpath.exceptions.BadRequestException;
import com.nexpath.models.User;
import com.nexpath.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    // 🔹 ONLY ADMIN
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminAccess() {
        return "Welcome Admin 🔥";
    }

    // 🔹 ONLY MENTOR
    @GetMapping("/mentor")
    @PreAuthorize("hasRole('MENTOR')")
    public String mentorAccess() {
        return "Welcome Mentor 🚀";
    }

    // 🔹 STUDENT + MENTOR + ADMIN
    @GetMapping("/common")
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','ADMIN')")
    public String commonAccess() {
        return "Common access 👥";
    }

    // 🔹 GET PROFILE
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Cacheable(value = "user_profile", key = "#userId")
    public ApiResponse<UserProfileDto> profile(@AuthenticationPrincipal Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserProfileDto dto = UserProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .profilePicture(user.getProfilePicture())
                .bio(user.getBio())
                .targetRole(user.getTargetRole())
                .skills(user.getSkills())
                .referralCode(user.getReferralCode())
                .build();

        return ApiResponse.success("Profile fetched", dto);
    }

    // 🔹 UPDATE PROFILE
    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(value = "user_profile", key = "#userId")
    public ApiResponse<UserProfileDto> updateProfile(
            @AuthenticationPrincipal Long userId,
            @RequestBody UpdateProfileRequest request) {
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (request.getName() != null) user.setName(request.getName());
        if (request.getBio() != null) user.setBio(request.getBio());
        if (request.getTargetRole() != null) user.setTargetRole(request.getTargetRole());
        if (request.getSkills() != null) user.setSkills(request.getSkills());

        userRepository.save(user);

        UserProfileDto dto = UserProfileDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .profilePicture(user.getProfilePicture())
                .bio(user.getBio())
                .targetRole(user.getTargetRole())
                .skills(user.getSkills())
                .referralCode(user.getReferralCode())
                .build();

        return ApiResponse.success("Profile updated", dto);
    }
}