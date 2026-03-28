package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentSessionRequest {

    @NotNull
    private Long orderId;
}
