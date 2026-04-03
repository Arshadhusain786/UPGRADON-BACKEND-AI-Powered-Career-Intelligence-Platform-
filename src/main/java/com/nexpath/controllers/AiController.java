package com.nexpath.controllers;

import com.nexpath.dtos.request.*;
import com.nexpath.dtos.response.*;
import com.nexpath.enums.CreditTransactionType;
import com.nexpath.exceptions.BadRequestException;
import com.nexpath.models.User;
import com.nexpath.repository.UserRepository;
import com.nexpath.services.AiService;
import com.nexpath.services.CreditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
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
        final int COST = 5;

        // Deduct upfront
        creditService.deductCredits(user, COST, CreditTransactionType.SPENT_ROADMAP,
                "AI Roadmap Generation");

        try {
            RoadmapResponse roadmap = aiService.generateRoadmap(request);
            return ApiResponse.success("Roadmap generated successfully", roadmap);
        } catch (Exception e) {
            // Refund on any failure
            log.error("Roadmap AI call failed for user {}: {}", userId, e.getMessage());
            creditService.addCredits(user, COST, CreditTransactionType.REFUND,
                    "Refund — AI Roadmap failed", null);
            throw new BadRequestException("AI service is temporarily unavailable. " +
                    COST + " credits have been refunded to your account.");
        }
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
        final int COST = 3;

        creditService.deductCredits(user, COST, CreditTransactionType.SPENT_SKILLGAP,
                "Skill Gap Analysis");

        try {
            SkillGapResponse response = aiService.analyzeSkillGap(request);
            return ApiResponse.success("Skill gap analyzed successfully", response);
        } catch (Exception e) {
            log.error("SkillGap AI call failed for user {}: {}", userId, e.getMessage());
            creditService.addCredits(user, COST, CreditTransactionType.REFUND,
                    "Refund — Skill Gap Analysis failed", null);
            throw new BadRequestException("AI service is temporarily unavailable. " +
                    COST + " credits have been refunded to your account.");
        }
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
        final int COST = 4;

        creditService.deductCredits(user, COST, CreditTransactionType.SPENT_RESUME,
                "Resume Score Analysis");

        try {
            ResumeScoreResponse response = aiService.scoreResume(request);
            return ApiResponse.success("Resume scored successfully", response);
        } catch (Exception e) {
            log.error("ResumeScore AI call failed for user {}: {}", userId, e.getMessage());
            creditService.addCredits(user, COST, CreditTransactionType.REFUND,
                    "Refund — Resume Score failed", null);
            throw new BadRequestException("AI service is temporarily unavailable. " +
                    COST + " credits have been refunded to your account.");
        }
    }

    // =========================
    // 📄 RESUME UPLOAD
    // =========================
    @PostMapping(value = "/resume-upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('STUDENT','MENTOR','ADMIN')")
    public ApiResponse<Map<String, Object>> resumeUpload(
            @AuthenticationPrincipal Long userId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "targetRole", required = false) String targetRole) {

        if (file.isEmpty()) throw new BadRequestException("File is empty");
        
        User user = loadUser(userId);
        final int COST = 4;

        creditService.deductCredits(user, COST, CreditTransactionType.SPENT_RESUME, "Resume Upload Analysis");

        try {
            String content;
            String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
            
            if (filename.endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(file.getBytes())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    content = stripper.getText(document);
                }
            } else {
                // Default fallback for .txt or other raw text files
                content = new String(file.getBytes(), StandardCharsets.UTF_8);
            }

            if (content == null || content.isBlank()) {
                throw new BadRequestException("Could not extract any text from the uploaded file.");
            }

            Map<String, Object> result = aiService.parseAndAnalyzeResume(content, targetRole);
            return ApiResponse.success("Resume analyzed successfully", result);

        } catch (Exception e) {
            log.error("Resume upload AI processing failed for user {}: {}", userId, e.getMessage());
            creditService.addCredits(user, COST, CreditTransactionType.REFUND, 
                    "Refund — Resume Upload Analysis failed", null);
            throw new BadRequestException("Failed to process resume: " + e.getMessage() + 
                    ". " + COST + " credits have been refunded.");
        }
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
}