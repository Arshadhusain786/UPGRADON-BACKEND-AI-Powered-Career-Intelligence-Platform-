package com.nexpath.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SkillGapRequest {
    @NotBlank
    private String currentSkills;
    @NotBlank private String targetRole;
}