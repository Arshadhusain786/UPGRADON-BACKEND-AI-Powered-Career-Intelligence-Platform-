package com.nexpath.services;

import com.nexpath.ai.RoadmapAIService;
import com.nexpath.dtos.request.*;
import com.nexpath.dtos.response.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiService {

    private final RoadmapAIService ai;

    // =========================
    // 🚀 ROADMAP
    // =========================
    public RoadmapResponse generateRoadmap(RoadmapRequest request) {

        Map<String, Object> map = ai.generateRoadmap(
                "Student",
                request.getTargetRole(),
                request.getExperienceLevel(),
                List.of(request.getCurrentSkills().split(",")),
                "3-6 months"
        );

        RoadmapResponse res = new RoadmapResponse();
        res.setTitle((String) map.get("title"));
        res.setSummary((String) map.get("summary"));
        res.setEstimatedDuration((String) map.get("estimatedDuration"));
        res.setPhases((List<Map<String, Object>>) map.get("phases"));

        return res;
    }

    // =========================
    // 🔍 SKILL GAP
    // =========================
    public SkillGapResponse analyzeSkillGap(SkillGapRequest request) {

        Map<String, Object> map =
                ai.analyzeSkillGap(request.getTargetRole(), request.getCurrentSkills());

        SkillGapResponse res = new SkillGapResponse();
        res.setMissingSkills((List<String>) map.get("missingSkills"));
        res.setStrengths((List<String>) map.get("strengths"));
        res.setRecommendations((List<String>) map.get("recommendations"));

        return res;
    }

    // =========================
    // 📄 RESUME SCORE
    // =========================
    public ResumeScoreResponse scoreResume(ResumeScoreRequest request) {

        Map<String, Object> map =
                ai.scoreResume(request.getResumeText(), "General Role");

        ResumeScoreResponse res = new ResumeScoreResponse();
        res.setScore((Integer) map.get("score"));
        res.setWeaknesses((List<String>) map.get("weaknesses"));
        res.setImprovements((List<String>) map.get("improvements"));
        res.setAtsSuggestions((List<String>) map.get("atsSuggestions"));
        res.setStrengths((List<String>) map.get("strengths"));

        return res;
    }
}