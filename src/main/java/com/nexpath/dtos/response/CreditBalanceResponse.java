package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreditBalanceResponse {

    private int totalCredits;
    private int freeTodayRemaining;
    private int totalEarned;
    private int totalSpent;
}
