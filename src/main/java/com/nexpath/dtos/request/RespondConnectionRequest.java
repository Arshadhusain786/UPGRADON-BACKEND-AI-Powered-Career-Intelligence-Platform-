package com.nexpath.dtos.request;

import com.nexpath.enums.ConnectionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RespondConnectionRequest {
    @NotNull(message = "Action is required")
    private ConnectionStatus action;
    
    private String posterNote;
}
