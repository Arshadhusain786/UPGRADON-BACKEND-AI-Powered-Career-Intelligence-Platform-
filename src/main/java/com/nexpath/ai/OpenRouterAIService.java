package com.nexpath.ai;

import com.nexpath.dtos.request.*;
import com.nexpath.dtos.response.*;
import com.nexpath.services.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenRouterAIService implements AiService {

    private final OpenAiChatModel chatModel;
    private final com.nexpath.repository.UserRepository userRepository;

    /**
     * Sanitizes user-supplied text before embedding into AI prompts.
     * Escapes characters that could break JSON templates or prompt parsing.
     */
    private String sanitize(String input) {
        if (input == null) return "";
        // Doubling the braces is the standard way to escape them in Spring AI templates
        String s = input
                .replace("{", "{{")
                .replace("}", "}}")
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\t", " ")
                .trim();
        // Allow more content if needed, but truncate to avoid context limit issues
        return s.substring(0, Math.min(s.length(), 6000));
    }

    private static final String PLATFORM_SYSTEM_PROMPT = """
        You are Upgradon Assistant — a helpful AI for the Upgradon career platform.
        Upgradon helps students and professionals bridge the industry skill gap using AI.
        
        Key features you know about:
        - AI Career Roadmap: generates step-by-step career plans (costs 5 credits)
        - Skill Gap Analysis: identifies missing skills for a target role (costs 3 credits)  
        - Resume Optimizer: scores resumes and gives ATS suggestions (costs 4 credits)
        - Resume Upload: upload PDF/DOCX for AI analysis (costs 4 credits)
        - Opportunity Board: job connection board with credit-based connections
        - Referral System: invite friends, earn bonus credits
        - Credit System: 20 free signup credits, 10 daily free credits
        
        Be helpful, concise, and encouraging. Always guide users toward features that
        help their career. If asked about pricing, mention the credit system.
        """;

    private ChatClient getChatClient() {
        return ChatClient.builder(chatModel).build();
    }

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public RoadmapResponse generateRoadmap(RoadmapRequest request) {
        String system = "You are an expert career coach. Return ONLY JSON matching the RoadmapResponse structure.";
        String user = String.format("Generate a career roadmap for target role: %s. Current Experience: %s. Current Skills: %s.",
                request.getTargetRole(), request.getExperienceLevel(), request.getCurrentSkills());

        try {
            return getChatClient().prompt()
                    .messages(List.of(new SystemMessage(system), new UserMessage(sanitize(user))))
                    .call()
                    .entity(RoadmapResponse.class);
        } catch (Exception e) {
            log.error("Roadmap AI call failed. Request: {}, Error: {}", request, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public SkillGapResponse analyzeSkillGap(SkillGapRequest request) {
        String system = "You are a tech recruiter. Return ONLY JSON matching the SkillGapResponse structure.";
        String user = String.format("Analyze the skill gap for target role: %s. Current Skills: %s.",
                request.getTargetRole(), request.getCurrentSkills());

        return getChatClient().prompt()
                .messages(List.of(new SystemMessage(system), new UserMessage(sanitize(user))))
                .call()
                .entity(SkillGapResponse.class);
    }

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public ResumeScoreResponse scoreResume(ResumeScoreRequest request) {
        String system = "You are an AI ATS (Applicant Tracking System). Return ONLY JSON matching the ResumeScoreResponse structure.";
        String user = String.format("Score this resume for the role: %s.\n\nResume Text:\n%s",
                request.getTargetRole(), request.getResumeText());

        return getChatClient().prompt()
                .messages(List.of(new SystemMessage(system), new UserMessage(sanitize(user))))
                .call()
                .entity(ResumeScoreResponse.class);
    }

    @Override
    public Map<String, Object> parseAndAnalyzeResume(String resumeText, String targetRole) {
        String system = "Analyze the resume and return ONLY JSON with these keys: score (0-100), extractedSkills (array of strings), improvements (array of strings), and weaknesses (array of strings).";
        String user = String.format("Analyze this resume for the role: %s.\n\nResume Content:\n%s",
                targetRole != null ? targetRole : "General", resumeText);

        return getChatClient().prompt()
                .messages(List.of(new SystemMessage(system), new UserMessage(sanitize(user))))
                .call()
                .entity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    }

    @Override
    public Flux<String> streamChat(Long userId, String userMessage) {
        com.nexpath.models.User user = userRepository.findById(userId).orElse(null);
        String context = PLATFORM_SYSTEM_PROMPT;
        if (user != null) {
            context += "\n\nUser: " + user.getName() + " (" + user.getRole() + ")";
        }
        return getChatClient().prompt()
                .messages(List.of(new SystemMessage(context), new UserMessage(sanitize(userMessage))))
                .stream()
                .content();
    }

    @Override
    public String chatPublic(String userMessage) {
        return getChatClient().prompt()
                .messages(List.of(new SystemMessage(PLATFORM_SYSTEM_PROMPT), new UserMessage(sanitize(userMessage))))
                .call()
                .content();
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        return getChatClient().prompt()
                .messages(List.of(new SystemMessage(systemPrompt), new UserMessage(sanitize(userMessage))))
                .call()
                .content();
    }
}
