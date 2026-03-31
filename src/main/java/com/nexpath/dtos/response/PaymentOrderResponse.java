package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentOrderResponse {

    private String orderId;
    private int amount;
    private String currency;
    private String key;
    private int credits;
    private String packageName;
}
