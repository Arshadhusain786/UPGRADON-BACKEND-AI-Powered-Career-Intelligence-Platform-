package com.nexpath.dtos.request;

import com.nexpath.enums.OpportunityType;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Company is required")
    private String company;
    
    @NotBlank(message = "Description is required")
    private String description;
    
    private OpportunityType roleType;
    private String location;
    private String skillsRequired;
    private String experienceRequired;
    private Integer maxConnections;
    private LocalDateTime expiresAt;
}
