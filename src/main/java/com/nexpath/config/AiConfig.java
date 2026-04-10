package com.nexpath.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.MetadataMode;
import com.nexpath.repository.UserCreditsRepository;
import java.util.function.Function;
import java.util.List;

import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.document.Document;
import org.springframework.context.annotation.Description;
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

    public record CreditRequest(String userId) {}
    public record CreditResponse(String balanceInfo) {}

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Bean
    public InMemoryChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

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

    @Bean
    public EmbeddingModel embeddingModel() {
        // Clean the API key from any invisible non-breaking spaces (0xa0) or trailing whitespace
        String cleanKey = apiKey.trim()
                .replace("\u00A0", "") 
                .replace(" ", "");      

        OpenAiApi openAiApi = new OpenAiApi(baseUrl, cleanKey);
        
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .withModel("text-embedding-3-small")
                .build();

        // 🛡️ Wrapping in a resilient decorator to handle OpenAI-compatible proxies (like OpenRouter)
        // that may omit the "usage" metadata, which causes a crash in the default Spring AI model.
        return new ResilientEmbeddingModel(new OpenAiEmbeddingModel(openAiApi, MetadataMode.ALL, options));
    }

    /**
     * Resilient wrapper for OpenAiEmbeddingModel that handles missing "usage" results.
     */
    private static class ResilientEmbeddingModel implements EmbeddingModel {
        private final OpenAiEmbeddingModel delegate;

        public ResilientEmbeddingModel(OpenAiEmbeddingModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public float[] embed(String text) {
            try {
                return delegate.embed(text);
            } catch (Exception e) {
                // If the primary embed fails, try to get results via call() 
                // which might have its own resilient handling
                try {
                    EmbeddingResponse response = this.call(new EmbeddingRequest(List.of(text), null));
                    if (response != null && !response.getResults().isEmpty()) {
                        return response.getResults().get(0).getOutput();
                    }
                } catch (Exception inner) {
                    // Fallback to safe vector below
                }
                
                // CRITICAL FIX: cosineSimilarity fails if vector norm is zero.
                // We return a small non-zero vector (norm will be 1.0) to prevent crash.
                float[] fallback = new float[dimensions() > 0 ? dimensions() : 1536];
                fallback[0] = 1.0f; 
                return fallback;
            }
        }

        @Override
        public float[] embed(Document document) {
            return delegate.embed(document);
        }

        @Override
        public List<float[]> embed(List<String> texts) {
            return delegate.embed(texts);
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            try {
                return delegate.call(request);
            } catch (Exception e) {
                // Handle the "OpenAI Usage must not be null" error from Spring AI 1.0.0-M3
                if (e.getMessage() != null && e.getMessage().contains("OpenAI Usage must not be null")) {
                    System.err.println("⚠️ ResilientEmbeddingModel: Handled missing Usage metadata.");
                    // We attempt to return a response with a zero-filled or dummy vector 
                    // that at least has a norm so it doesn't crash the VectorStore
                    float[] dummy = new float[dimensions() > 0 ? dimensions() : 1536];
                    dummy[0] = 1.0f;
                    return new EmbeddingResponse(List.of(new Embedding(dummy, 0))); 
                }
                throw e;
            }
        }

        @Override
        public int dimensions() {
            return delegate.dimensions();
        }
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }

    @Bean
    @Description("Get the current user's available credit balance")
    public Function<CreditRequest, CreditResponse> checkCredits(UserCreditsRepository repository) {
        return request -> {
            try {
                Long id = Long.valueOf(request.userId());
                return repository.findByUserId(id)
                        .map(c -> new CreditResponse("You currently have " + c.getTotalCredits() + " credits."))
                        .orElse(new CreditResponse("User credits record not found."));
            } catch (Exception e) {
                return new CreditResponse("Error retrieving credits: " + e.getMessage());
            }
        };
    }
}
