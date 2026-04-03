package com.nexpath.services;

import com.nexpath.dtos.request.*;
import com.nexpath.dtos.response.*;
import reactor.core.publisher.Flux;
import java.util.Map;

public interface AiService {

    /**
     * 🚀 Generates a career roadmap based on target role and skills.
     */
    RoadmapResponse generateRoadmap(RoadmapRequest request);

    /**
     * 🔍 Analyzes the gap between current skills and target role.
     */
    SkillGapResponse analyzeSkillGap(SkillGapRequest request);

    /**
     * 📄 Scores a resume text and providing improvement feedback.
     */
    ResumeScoreResponse scoreResume(ResumeScoreRequest request);

    /**
     * 📄 Unified resume parsing and analysis from a file.
     */
    Map<String, Object> parseAndAnalyzeResume(String resumeText, String targetRole);

    /**
     * 🌊 Streaming chat interface for authenticated users (Personalized).
     */
    Flux<String> streamChat(Long userId, String userMessage);

    /**
     * 💬 Public chat interface (Generic).
     */
    String chatPublic(String userMessage);

    String chat(String s, String s1);
}