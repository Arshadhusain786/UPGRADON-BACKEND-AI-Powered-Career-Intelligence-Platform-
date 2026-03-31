package com.nexpath.mapper;

import com.nexpath.dtos.UserDto;
import com.nexpath.models.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public static UserDto toDto(User user) {
        if (user == null) return null;

        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().name() : null,
                user.getProfilePicture(),
                user.getBio(),
                user.getTheme(),
                user.isEmailVerified(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null
        );
    }
}