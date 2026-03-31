package com.nexpath.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PurchaseRequest {

    @NotBlank(message = "Package name is required")
    private String packageName;
}
