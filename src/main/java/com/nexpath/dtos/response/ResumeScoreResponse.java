package com.nexpath.dtos.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class ResumeScoreResponse {

    private Integer score;
    private List<String> weaknesses;
    private List<String> improvements;
    private List<String> atsSuggestions;
    private List<String> strengths;

    // ✅ Construct from AI-parsed Map
    @SuppressWarnings("unchecked")
    public ResumeScoreResponse(Map<String, Object> data) {
        Object rawScore      = data.get("score");
        this.score           = (rawScore instanceof Number) ? ((Number) rawScore).intValue() : 0;
        this.weaknesses      = (List<String>) data.getOrDefault("weaknesses",     List.of());
        this.improvements    = (List<String>) data.getOrDefault("improvements",   List.of());
        this.atsSuggestions  = (List<String>) data.getOrDefault("atsSuggestions", List.of());
        this.strengths       = (List<String>) data.getOrDefault("strengths",      List.of());
    }
}