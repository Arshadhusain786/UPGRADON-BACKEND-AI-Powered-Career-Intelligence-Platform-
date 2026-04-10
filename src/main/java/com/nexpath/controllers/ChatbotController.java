package com.nexpath.controllers;

import com.nexpath.dtos.response.ApiResponse;
import com.nexpath.enums.CreditTransactionType;
import com.nexpath.exceptions.BadRequestException;
import com.nexpath.models.User;
import com.nexpath.repository.UserRepository;
import com.nexpath.services.AiService;
import com.nexpath.services.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final AiService aiService;
    private final CreditService creditService;
    private final UserRepository userRepository;

    // Public — landing page chatbot
    @PostMapping("/public")
    public ApiResponse<Map<String, String>> publicChat(
            @RequestBody Map<String, String> body) {
        String message = body.get("message");
        String sessionId = body.get("sessionId");
        if (message == null || message.isBlank()) throw new BadRequestException("Message is required");
        
        try {
            String response = (sessionId != null && !sessionId.isBlank()) 
                ? aiService.chatPublicWithSession(sessionId, message)
                : aiService.chatPublic(message);
            
            return ApiResponse.success("Response generated", Map.of("reply", response != null ? response : "I'm having trouble responding right now."));
        } catch (Exception e) {
            log.error("Public chat failed: {}", e.getMessage(), e);
            return ApiResponse.error("AI Service Error: " + e.getMessage());
        }
    }

    // Authenticated — deducts 1 credit
    @PostMapping("/message")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Map<String, String>> authenticatedChat(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.isBlank()) throw new BadRequestException("Message is required");

        User user = loadUser(userId);
        // Free chatbot - no deduction
        
        try {
            String response = aiService.chatPublic(message + "\n\nUser: " + user.getName()); 
            return ApiResponse.success("Response generated", Map.of("reply", response));
        } catch (Exception e) {
            log.error("Authenticated chat failed for user {}: {}", userId, e.getMessage(), e);
            throw new BadRequestException("AI Service Error: " + e.getMessage());
        }
    }

    // Streaming endpoint (SSE)
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Flux<String> streamChat(
            @AuthenticationPrincipal Long userId,
            @RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.isBlank()) return Flux.just("ERROR: Message required");
        
        return aiService.streamChat(userId, message)
                .onErrorResume(e -> {
                    log.error("Stream chat failed for user {}: {}", userId, e.getMessage(), e);
                    return Flux.just("\n\n[ERROR: AI Service is temporarily unavailable. Please try again later.]");
                });
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));
    }
}
