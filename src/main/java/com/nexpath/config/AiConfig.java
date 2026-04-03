package com.nexpath.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Custom AI Configuration to fix a common issue where API keys copied from
 * browser dashboards (like OpenRouter) contain invisible non-breaking spaces (0xa0),
 * causing "Unexpected char 0xa0" in the Authorization header.
 * 
 * Also configures default token limits to prevent "402 Payment Required" on low balance.
 */
@Configuration
public class AiConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Bean
    @Primary
    public OpenAiChatModel openAiChatModel() {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("OPENROUTER_API_KEY is not set.");
        }

        // Clean the API key from any invisible non-breaking spaces (0xa0) or trailing whitespace
        String cleanKey = apiKey.trim()
                .replace("\u00A0", "") 
                .replace(" ", "");      

        OpenAiApi openAiApi = new OpenAiApi(baseUrl, cleanKey);
        
        // Define default options to avoid requesting the default 16k tokens (which causes 402 Error on low credits)
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .withModel("google/gemini-2.0-flash-001") // Fallback model if not specified
                .withMaxTokens(2048)
                .withTemperature(0.7)
                .build();

        return new OpenAiChatModel(openAiApi, options);
    }
}
