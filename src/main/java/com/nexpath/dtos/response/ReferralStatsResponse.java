package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralStatsResponse {
    private String referralCode;
    private String referralLink;
    private long totalReferrals;
    private int totalEarned;
    private List<ReferralDto> referrals;
}
