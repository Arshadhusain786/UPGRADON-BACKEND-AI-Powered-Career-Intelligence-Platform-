package com.nexpath.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResumeScoreRequest {
    @NotBlank
    private String resumeText;
    
    private String targetRole;
}