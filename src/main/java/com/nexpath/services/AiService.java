package com.nexpath.services;


import com.nexpath.ai.RoadmapAIService;

import com.nexpath.dtos.request.ResumeScoreRequest;
import com.nexpath.dtos.request.RoadmapRequest;
import com.nexpath.dtos.request.SkillGapRequest;
import com.nexpath.dtos.response.ResumeScoreResponse;
import com.nexpath.dtos.response.RoadmapResponse;
import com.nexpath.dtos.response.SkillGapResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final RoadmapAIService roadmapAIService;

    // 🔹 ROADMAP
    public RoadmapResponse generateRoadmap(RoadmapRequest request) {

        Map<String, Object> result = roadmapAIService.generateRoadmap(
                "Student", // current role (can improve later)
                request.getTargetRole(),
                request.getExperienceLevel(),
                List.of(request.getCurrentSkills().split(",")),
                "3-6 months"
        );

        return new RoadmapResponse(result);
    }

    // 🔹 SKILL GAP
    public SkillGapResponse analyzeSkillGap(SkillGapRequest request) {

        Map<String, Object> result = roadmapAIService.analyzeSkillGap(
                request.getTargetRole(),
                request.getCurrentSkills()
        );

        return new SkillGapResponse(result);
    }

    // 🔹 RESUME SCORE
    public ResumeScoreResponse scoreResume(ResumeScoreRequest request) {

        Map<String, Object> result = roadmapAIService.scoreResume(
                request.getResumeText(),
                "General Role"
        );

        return new ResumeScoreResponse(result);
    }
}