package com.nexpath.controllers;

import com.nexpath.dtos.request.CreatePostRequest;
import com.nexpath.dtos.request.JobEnhanceRequest;
import com.nexpath.dtos.response.ApiResponse;
import com.nexpath.dtos.response.JobEnhanceResponse;
import com.nexpath.dtos.response.OpportunityPostResponse;
import com.nexpath.enums.CreditTransactionType;
import com.nexpath.models.User;
import com.nexpath.repository.UserRepository;
import com.nexpath.services.AiService;
import com.nexpath.services.CreditService;
import com.nexpath.services.OpportunityPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/opportunities")
@RequiredArgsConstructor
@Slf4j
public class OpportunityPostController {

    private final OpportunityPostService postService;
    private final AiService aiService;
    private final CreditService creditService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<OpportunityPostResponse> createPost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreatePostRequest req) {
        log.info("Received request to create opportunity: {} from user: {}", req.getTitle(), userId);
        return ApiResponse.success("Opportunity posted", postService.createPost(userId, req));
    }

    @PostMapping("/enhance")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<JobEnhanceResponse> enhancePost(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody JobEnhanceRequest req) {
        log.info("Enhancing job post: {} for user: {}", req.getTitle(), userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Deduct 1 credit for enhancement
        creditService.deductCredits(user, 1, CreditTransactionType.AI_GENERATION, "Job description enhancement");

        JobEnhanceResponse response = aiService.enhanceJobDescription(req);
        return ApiResponse.success("Description enhanced", response);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<OpportunityPostResponse>> getActivePosts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type) {
        return ApiResponse.success("Posts loaded", postService.getActivePosts(userId, search, type, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<OpportunityPostResponse> getPost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ApiResponse.success("Post loaded", postService.getPost(id, userId));
    }

    @GetMapping("/my-posts")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<OpportunityPostResponse>> getMyPosts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("My posts", postService.getMyPosts(userId, page, size));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<OpportunityPostResponse> updatePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody CreatePostRequest req) {
        return ApiResponse.success("Post updated", postService.updatePost(userId, id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> closePost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        postService.closePost(userId, id);
        return ApiResponse.success("Post closed", null);
    }

    @PostMapping("/{id}/boost")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> boostPost(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        postService.boostPost(userId, id);
        return ApiResponse.success("Post boosted for 7 days!", null);
    }
}
