package com.nexpath.dtos.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class SkillGapResponse {
//add
    private List<String> missingSkills;
    private List<String> strengths;
    private List<String> recommendations;

    // ✅ Construct from AI-parsed Map
    @SuppressWarnings("unchecked")
    public SkillGapResponse(Map<String, Object> data) {
        this.missingSkills    = (List<String>) data.getOrDefault("missingSkills",    List.of());
        this.strengths        = (List<String>) data.getOrDefault("strengths",        List.of());
        this.recommendations  = (List<String>) data.getOrDefault("recommendations",  List.of());
    }
}