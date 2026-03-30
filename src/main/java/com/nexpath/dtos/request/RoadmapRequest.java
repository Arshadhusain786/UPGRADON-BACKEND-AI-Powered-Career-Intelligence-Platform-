package com.nexpath.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RoadmapRequest {
    @NotBlank
    private String currentSkills;
    @NotBlank private String targetRole;
    private String experienceLevel;
}