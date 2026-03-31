package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpportunityPostResponse {
    private Long id;
    private String title;
    private String company;
    private String description;
    private String roleType;
    private String location;
    private String skillsRequired;
    private String experienceRequired;
    private String status;
    private boolean isBoosted;
    private int viewCount;
    private int connectionCount;
    private int maxConnections;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private boolean hasRequested;
    private PosterInfo poster;

    @Data
    @AllArgsConstructor
    public static class PosterInfo {
        private Long id;
        private String name;
        private String profilePicture;
        private String role;
    }
}
