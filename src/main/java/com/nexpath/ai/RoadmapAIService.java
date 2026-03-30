package com.nexpath.ai;

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
    // ROADMAP
    // ==============================
    public Map<String, Object> generateRoadmap(
            String currentRole,
            String targetRole,
            String experience,
            List<String> skills,
            String timeline
    ) {

        String prompt = """
        You are a senior career mentor.

        Generate a structured JSON roadmap.

        Return ONLY JSON in this format:
        {
          "title": "",
          "summary": "",
          "phases": [
            {
              "phase": "",
              "topics": []
            }
          ]
        }

        Current Role: %s
        Target Role: %s
        Experience: %s
        Skills: %s
        Timeline: %s
        """.formatted(currentRole, targetRole, experience, skills, timeline);

        String response = callOpenRouter(prompt);
        return parseJson(response);
    }

    // ==============================
    // SKILL GAP
    // ==============================
    public Map<String, Object> analyzeSkillGap(String targetRole, String currentSkills) {

        String prompt = """
        Analyze skill gap.

        Return JSON:
        {
          "missingSkills": [],
          "strengths": [],
          "recommendations": []
        }

        Target Role: %s
        Current Skills: %s
        """.formatted(targetRole, currentSkills);

        String response = callOpenRouter(prompt);
        return parseJson(response);
    }

    // ==============================
    // RESUME SCORE
    // ==============================
    public Map<String, Object> scoreResume(String resumeText, String targetRole) {

        String prompt = """
       You are an ATS resume evaluator.

       STRICT RULES:
      - Return ONLY valid JSON
      - DO NOT add explanation
      - DO NOT add text outside JSON
      - score must be between 0 and 100

      Format:
     {
      "score": number,
      "feedback": ["point1", "point2"],
      "improvements": ["point1", "point2"]
     }

     Evaluate this resume:

     %s
     """.formatted(resumeText);
        String response = callOpenRouter(prompt);
        return parseJson(response);
    }

    // ==============================
    // OPENROUTER CALL
    // ==============================
    private String callOpenRouter(String prompt) {

        try {
            log.info("OpenRouter Key Loaded: {}", apiKey != null && !apiKey.isBlank());

            String requestBody = objectMapper.writeValueAsString(
                    Map.of(
                            "model", "openai/gpt-3.5-turbo",
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

            log.info("Calling OpenRouter API...");

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            log.info("OpenRouter Response received");
            log.debug("Raw Response: {}", response.body());

            Map<String, Object> json = objectMapper.readValue(response.body(), Map.class);

            // 🔥 HANDLE ERROR RESPONSE
            if (json.containsKey("error")) {
                log.error("OpenRouter API Error: {}", json.get("error"));
                return getFallbackResponse();
            }

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) json.get("choices");

            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");

            return message.get("content").toString();

        } catch (Exception e) {
            log.error("OpenRouter API error", e);
            return getFallbackResponse();
        }
    }

    // ==============================
    // JSON PARSER
    // ==============================
    private Map<String, Object> parseJson(String response) {
        try {
            return objectMapper.readValue(response, Map.class);
        } catch (Exception e) {
            log.warn("Invalid JSON from AI, returning fallback");
            return Map.of(
                    "title", "Fallback Roadmap",
                    "summary", "AI response could not be parsed",
                    "phases", List.of()
            );
        }
    }

    // ==============================
    // FALLBACK RESPONSE
    // ==============================
    private String getFallbackResponse() {
        return """
        {
          "title": "Fallback Roadmap",
          "summary": "AI service temporarily unavailable",
          "phases": []
        }
        """;
    }
}