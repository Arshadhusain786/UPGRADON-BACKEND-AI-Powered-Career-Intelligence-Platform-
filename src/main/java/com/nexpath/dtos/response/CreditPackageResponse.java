package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreditPackageResponse {

    private String name;
    private String displayName;
    private int amountPaise;
    private int credits;
    private String priceDisplay;
}
