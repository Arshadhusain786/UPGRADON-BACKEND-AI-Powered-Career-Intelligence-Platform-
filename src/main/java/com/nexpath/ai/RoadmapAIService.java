package com.nexpath.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoadmapAIService {

    @Value("${openrouter.api-key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // ==============================
    // 🚀 ROADMAP
    // ==============================
    public Map<String, Object> generateRoadmap(
            String currentRole,
            String targetRole,
            String experience,
            List<String> skills,
            String timeline
    ) {
        String prompt = """
        Return ONLY JSON.

        {
          "title": "",
          "summary": "",
          "estimatedDuration": "",
          "phases": []
        }

        Current Role: %s
        Target Role: %s
        Experience: %s
        Skills: %s
        Timeline: %s
        """.formatted(currentRole, targetRole, experience, skills, timeline);

        return parseJson(callWithFallback(prompt));
    }

    // ==============================
    // 🔍 SKILL GAP
    // ==============================
    public Map<String, Object> analyzeSkillGap(String targetRole, String currentSkills) {

        String prompt = """
        Return ONLY JSON:

        {
          "missingSkills": [],
          "strengths": [],
          "recommendations": []
        }

        Target Role: %s
        Current Skills: %s
        """.formatted(targetRole, currentSkills);

        return parseJson(callWithFallback(prompt));
    }

    // ==============================
    // 📄 RESUME SCORE
    // ==============================
    public Map<String, Object> scoreResume(String resumeText, String targetRole) {

        String prompt = """
        Return ONLY JSON:

        {
          "score": 0,
          "weaknesses": [],
          "improvements": [],
          "atsSuggestions": [],
          "strengths": []
        }

        Target Role: %s
        Resume: %s
        """.formatted(targetRole, resumeText);

        return parseJson(callWithFallback(prompt));
    }

    // ==============================
    // 🔥 FALLBACK + RETRY
    // ==============================
    private String callWithFallback(String prompt) {

        List<String> models = List.of(
                "openai/gpt-4o-mini",
                "mistral/mistral-large"
        );

        for (String model : models) {
            try {
                return callOpenRouter(model, prompt);
            } catch (Exception e) {
                log.warn("Model failed: {}", model);
            }
        }

        return getFallbackResponse();
    }

    // ==============================
    // 🌐 OPENROUTER CALL
    // ==============================
    private String callOpenRouter(String model, String prompt) throws Exception {

        String requestBody = objectMapper.writeValueAsString(
                Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "user", "content", prompt)
                        )
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        JsonNode root = objectMapper.readTree(response.body());

        return root
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText();
    }

    // ==============================
    // 🧠 PARSER
    // ==============================
    private Map<String, Object> parseJson(String response) {
        try {
            log.debug("🔍 Raw AI Response: {}", response);

            // 🔥 STEP 1: Remove markdown
            String cleaned = response.trim();

            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json", "")
                        .replaceAll("```", "")
                        .trim();
            }

            // 🔥 STEP 2: Extract ONLY JSON object (important fix)
            int start = cleaned.indexOf("{");
            int end = cleaned.lastIndexOf("}");

            if (start == -1 || end == -1) {
                throw new RuntimeException("Invalid JSON format");
            }

            cleaned = cleaned.substring(start, end + 1);

            // 🔥 STEP 3: Parse safely
            Map<String, Object> parsed = objectMapper.readValue(cleaned, Map.class);

            log.info("✅ JSON parsed successfully");

            return parsed;

        } catch (Exception e) {
            log.error("❌ JSON parsing failed", e);

            return Map.of(
                    "title", "Error generating roadmap",
                    "summary", "AI returned invalid data. Please retry.",
                    "estimatedDuration", "N/A",
                    "phases", List.of()
            );
        }
    }
    private String getFallbackResponse() {
        return "{}";
    }
}