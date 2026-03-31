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
public class ReferralDto {
    private String name;
    private String email;
    private int rewardCredits;
    private LocalDateTime joinedAt;
}
