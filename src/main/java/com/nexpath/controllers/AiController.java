package com.nexpath.controllers;

import com.nexpath.dtos.request.ResumeScoreRequest;
import com.nexpath.dtos.request.RoadmapRequest;
import com.nexpath.dtos.request.SkillGapRequest;
import com.nexpath.dtos.response.ApiResponse;
import com.nexpath.dtos.response.ResumeScoreResponse;
import com.nexpath.dtos.response.RoadmapResponse;
import com.nexpath.dtos.response.SkillGapResponse;
import com.nexpath.services.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    // =========================
    // 🚀 ROADMAP
    // =========================
    @PostMapping("/roadmap")
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','ADMIN')")
    public ApiResponse<RoadmapResponse> roadmap(@Valid @RequestBody RoadmapRequest request) {

        RoadmapResponse roadmap = aiService.generateRoadmap(request);

        return ApiResponse.success(
                "Roadmap generated successfully",
                roadmap
        );
    }

    // =========================
    // 🔍 SKILL GAP
    // =========================
    @PostMapping("/skill-gap")
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','ADMIN')")
    public ApiResponse<SkillGapResponse> skillGap(@Valid @RequestBody SkillGapRequest request) {

        SkillGapResponse response = aiService.analyzeSkillGap(request);

        return ApiResponse.success(
                "Skill gap analyzed successfully",
                response
        );
    }

    // =========================
    // 📄 RESUME SCORE
    // =========================
    @PostMapping("/resume-score")
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','ADMIN')")
    public ApiResponse<ResumeScoreResponse> resumeScore(@Valid @RequestBody ResumeScoreRequest request) {

        ResumeScoreResponse response = aiService.scoreResume(request);

        return ApiResponse.success(
                "Resume scored successfully",
                response
        );
    }
}