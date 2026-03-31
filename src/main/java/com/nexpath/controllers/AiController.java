package com.nexpath.controllers;

import com.nexpath.dtos.request.ResumeScoreRequest;
import com.nexpath.dtos.request.RoadmapRequest;
import com.nexpath.dtos.request.SkillGapRequest;
import com.nexpath.dtos.response.ApiResponse;
import com.nexpath.dtos.response.ResumeScoreResponse;
import com.nexpath.dtos.response.RoadmapResponse;
import com.nexpath.dtos.response.SkillGapResponse;
import com.nexpath.enums.CreditTransactionType;
import com.nexpath.exceptions.BadRequestException;
import com.nexpath.models.User;
import com.nexpath.repository.UserRepository;
import com.nexpath.services.AiService;
import com.nexpath.services.CreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final CreditService creditService;
    private final UserRepository userRepository;

    // =========================
    // 🚀 ROADMAP
    // =========================
    @PostMapping("/roadmap")
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','ADMIN')")
    public ApiResponse<RoadmapResponse> roadmap(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody RoadmapRequest request) {

        User user = loadUser(userId);
        
        // 💎 Deduct Credits (5)
        creditService.deductCredits(user, 5, CreditTransactionType.SPENT_ROADMAP, "AI Roadmap Generation");

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
    public ApiResponse<SkillGapResponse> skillGap(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SkillGapRequest request) {

        User user = loadUser(userId);
        
        // 💎 Deduct Credits (3)
        creditService.deductCredits(user, 3, CreditTransactionType.SPENT_SKILLGAP, "Skill Gap Analysis");

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
    public ApiResponse<ResumeScoreResponse> resumeScore(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ResumeScoreRequest request) {

        User user = loadUser(userId);
        
        // 💎 Deduct Credits (4)
        creditService.deductCredits(user, 4, CreditTransactionType.SPENT_RESUME, "Resume Score Analysis");

        ResumeScoreResponse response = aiService.scoreResume(request);

        return ApiResponse.success(
                "Resume scored successfully",
                response
        );
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
}