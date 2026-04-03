package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RoadmapResponse {
    private String title;
    private String summary;
    private String estimatedDuration;
    private List<Map<String, Object>> phases;

    // ✅ Constructor from AI-parsed Map so AiService can populate this correctly
    @SuppressWarnings("unchecked")
    public RoadmapResponse(Map<String, Object> data) {
        this.title             = data.getOrDefault("title", "Career Roadmap").toString();
        this.summary           = data.getOrDefault("summary", "").toString();
        this.estimatedDuration = data.getOrDefault("estimatedDuration", "N/A").toString();
        Object rawPhases       = data.get("phases");
        this.phases            = (rawPhases instanceof List) ? (List<Map<String, Object>>) rawPhases : List.of();
    }
}