package com.nexpath.dtos.request;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String name;
    private String email;
    private String bio;
    private String targetRole;
    private String skills;
    private String theme;
}
