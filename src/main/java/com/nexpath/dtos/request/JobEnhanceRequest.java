package com.nexpath.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobEnhanceRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Raw description is required")
    private String description;
}
