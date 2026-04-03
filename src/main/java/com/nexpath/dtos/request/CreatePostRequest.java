package com.nexpath.dtos.request;

import com.nexpath.enums.OpportunityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreatePostRequest {
    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 120, message = "Title must be between 3 and 120 characters")
    private String title;

    @NotBlank(message = "Company is required")
    @Size(max = 100, message = "Company name too long")
    private String company;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 5000, message = "Description must be between 20 and 5000 characters")
    private String description;

    private OpportunityType roleType;

    @Size(max = 100)
    private String location;

    @Size(max = 500)
    private String skillsRequired;

    @Size(max = 100)
    private String experienceRequired;

    private Integer maxConnections;
    private LocalDateTime expiresAt;
}
