package com.nexpath.dtos.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendConnectionRequest {
    @NotNull(message = "Post ID is required")
    private Long postId;
    
    private String message;
}
