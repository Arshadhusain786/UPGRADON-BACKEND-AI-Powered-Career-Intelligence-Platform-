package com.nexpath.dtos.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileDto {
    private Long id;
    private String name;
    private String email;
    private String role;
    private String profilePicture;
    private String bio;
    private String targetRole;
    private String skills;
    private String referralCode;
}
