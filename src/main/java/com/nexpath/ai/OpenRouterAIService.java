package com.nexpath.ai;

import com.nexpath.dtos.request.*;
import com.nexpath.dtos.response.*;
import com.nexpath.services.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenRouterAIService implements AiService {

    private final OpenAiChatModel chatModel;
    private final com.nexpath.repository.UserRepository userRepository;
    private final InMemoryChatMemory chatMemory;
    private final VectorStore vectorStore;

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
    @Cacheable(value = "career_roadmaps", key = "#request.targetRole + #request.experienceLevel + #request.currentSkills")
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public RoadmapResponse generateRoadmap(RoadmapRequest request) {
        try {
            PromptTemplate template = new PromptTemplate(new ClassPathResource("prompts/roadmap.st"));
            Map<String, Object> params = Map.of(
                "targetRole", request.getTargetRole(),
                "experienceLevel", request.getExperienceLevel(),
                "currentSkills", request.getCurrentSkills()
            );

            return getChatClient().prompt(template.create(params))
                    .call()
                    .entity(RoadmapResponse.class);
        } catch (Exception e) {
            log.error("Roadmap AI call failed. Request: {}, Error: {}", request, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Cacheable(value = "skill_gaps", key = "#request.targetRole + #request.currentSkills")
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public SkillGapResponse analyzeSkillGap(SkillGapRequest request) {
        PromptTemplate template = new PromptTemplate(new ClassPathResource("prompts/skillgap.st"));
        Map<String, Object> params = Map.of(
            "targetRole", request.getTargetRole(),
            "currentSkills", request.getCurrentSkills()
        );

        return getChatClient().prompt(template.create(params))
                .call()
                .entity(SkillGapResponse.class);
    }

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public ResumeScoreResponse scoreResume(ResumeScoreRequest request) {
        PromptTemplate template = new PromptTemplate(new ClassPathResource("prompts/resumescore.st"));
        Map<String, Object> params = Map.of(
            "targetRole", request.getTargetRole(),
            "resumeText", sanitize(request.getResumeText())
        );

        return getChatClient().prompt(template.create(params))
                .call()
                .entity(ResumeScoreResponse.class);
    }

    @Override
    public Map<String, Object> parseAndAnalyzeResume(String resumeText, String targetRole) {
        PromptTemplate template = new PromptTemplate(new ClassPathResource("prompts/resumeparse.st"));
        Map<String, Object> params = Map.of(
            "targetRole", targetRole != null ? targetRole : "General",
            "resumeText", sanitize(resumeText)
        );

        return getChatClient().prompt(template.create(params))
                .call()
                .entity(new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
    }

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public JobEnhanceResponse enhanceJobDescription(JobEnhanceRequest request) {
        PromptTemplate template = new PromptTemplate(new ClassPathResource("prompts/jobenhance.st"));
        Map<String, Object> params = Map.of(
            "title", request.getTitle(),
            "description", sanitize(request.getDescription())
        );

        return getChatClient().prompt(template.create(params))
                .call()
                .entity(JobEnhanceResponse.class);
    }

    @Override
    public Flux<String> streamChat(Long userId, String userMessage) {
        String context = String.format("%s\n\n[USER CONTEXT]\n- User ID: %s\n- Real-time Actions: You can check the user's credit balance using the 'checkCredits' function.", PLATFORM_SYSTEM_PROMPT, userId);
        String conversationId = "chat-" + userId;

        try {
            // Attempt RAG-powered chat first
            return ChatClient.builder(chatModel)
                    .defaultAdvisors(
                            new MessageChatMemoryAdvisor(chatMemory, conversationId, 10),
                            new QuestionAnswerAdvisor(vectorStore)
                    )
                    .build()
                    .prompt()
                    .messages(List.of(new SystemMessage(context), new UserMessage(sanitize(userMessage))))
                    .stream()
                    .content()
                    .onErrorResume(e -> {
                        log.warn("RAG stream failed, falling back to general chat: {}", e.getMessage());
                        // Fallback: Continue without RAG advisor
                        return getChatClient().prompt()
                                .messages(List.of(new SystemMessage(context), new UserMessage(sanitize(userMessage))))
                                .stream()
                                .content();
                    });
        } catch (Exception e) {
            log.error("Fatal error creating ChatClient: {}", e.getMessage());
            return Flux.just("\n\n[ERROR: AI Service is temporarily unavailable. Please try again later.]");
        }
    }

    @Override
    public String chatPublic(String userMessage) {
        try {
            return getChatClient().prompt()
                    .messages(List.of(new SystemMessage(PLATFORM_SYSTEM_PROMPT), new UserMessage(sanitize(userMessage))))
                    .call()
                    .content();
        } catch (Exception e) {
            log.error("AI chatPublic failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String chatPublicWithSession(String sessionId, String userMessage) {
        try {
            String conversationId = "public-" + sessionId;
            try {
                // Try RAG first
                return ChatClient.builder(chatModel)
                        .defaultAdvisors(
                                new MessageChatMemoryAdvisor(chatMemory, conversationId, 5),
                                new QuestionAnswerAdvisor(vectorStore)
                        )
                        .build()
                        .prompt()
                        .messages(List.of(new SystemMessage(PLATFORM_SYSTEM_PROMPT), new UserMessage(sanitize(userMessage))))
                        .call()
                        .content();
            } catch (Exception ragEx) {
                log.warn("Public RAG chat failed, falling back to general: {}", ragEx.getMessage());
                return getChatClient().prompt()
                        .messages(List.of(new SystemMessage(PLATFORM_SYSTEM_PROMPT), new UserMessage(sanitize(userMessage))))
                        .call()
                        .content();
            }
        } catch (Exception e) {
            log.error("AI chatPublicWithSession failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public String chat(String systemPrompt, String userMessage) {
        return getChatClient().prompt()
                .messages(List.of(new SystemMessage(systemPrompt), new UserMessage(sanitize(userMessage))))
                .call()
                .content();
    }
}
