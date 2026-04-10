package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEnhanceResponse {
    private String enhancedDescription;
    private List<String> keyRequirements;
    private List<String> benefits;
}
