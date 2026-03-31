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
public class ConnectionRequestResponse {
    private Long id;
    private String status;
    private String message;
    private boolean creditLocked;
    private boolean isFreeRequest;
    private String seekerNote;
    private String posterNote;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime hiredAt;
    private SeekerInfo seeker;
    private PostSummary post;

    @Data
    @AllArgsConstructor
    public static class SeekerInfo {
        private Long id;
        private String name;
        private String email;
        private String profilePicture;
        private String role;
    }

    @Data
    @AllArgsConstructor
    public static class PostSummary {
        private Long id;
        private String title;
        private String company;
    }
}
