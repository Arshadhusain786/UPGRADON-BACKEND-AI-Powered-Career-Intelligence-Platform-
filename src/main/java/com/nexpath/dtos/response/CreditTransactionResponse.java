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
public class CreditTransactionResponse {
    private Long id;
    private String type;
    private int amount;
    private String description;
    private String referenceId;
    private int balanceAfter;
    private LocalDateTime createdAt;
}
