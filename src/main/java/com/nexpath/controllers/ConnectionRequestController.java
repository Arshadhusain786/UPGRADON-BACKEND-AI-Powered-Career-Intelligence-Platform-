package com.nexpath.controllers;

import com.nexpath.dtos.request.RespondConnectionRequest;
import com.nexpath.dtos.request.SendConnectionRequest;
import com.nexpath.dtos.response.ApiResponse;
import com.nexpath.dtos.response.ConnectionRequestResponse;
import com.nexpath.dtos.response.JobDashboardResponse;
import com.nexpath.services.ConnectionRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/connections")
@RequiredArgsConstructor
public class ConnectionRequestController {

    private final ConnectionRequestService connService;

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ConnectionRequestResponse> sendRequest(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SendConnectionRequest req) {
        return ApiResponse.success("Connection request sent", connService.sendRequest(userId, req));
    }

    @PostMapping("/{id}/respond")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ConnectionRequestResponse> respondToRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id,
            @Valid @RequestBody RespondConnectionRequest req) {
        return ApiResponse.success("Response recorded", connService.respondToRequest(userId, id, req));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> cancelRequest(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        connService.cancelRequest(userId, id);
        return ApiResponse.success("Request cancelled. Credit refunded if applicable.", null);
    }

    @PostMapping("/{id}/mark-hired")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<ConnectionRequestResponse> markAsHired(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        return ApiResponse.success("Marked as hired! +5 platform reward credits added.", 
                connService.markAsHired(userId, id));
    }

    @GetMapping("/sent")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<ConnectionRequestResponse>> getMySentRequests(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Sent requests", connService.getMySentRequests(userId, page, size));
    }

    @GetMapping("/received")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<ConnectionRequestResponse>> getRequestsForMyPosts(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success("Received requests", connService.getRequestsForMyPosts(userId, page, size));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<JobDashboardResponse> getJobDashboard(
            @AuthenticationPrincipal Long userId) {
        return ApiResponse.success("Job dashboard", connService.getJobDashboard(userId));
    }
}
