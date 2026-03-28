package com.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrderCreateRequest {

    @NotBlank(message = "Shipping address is required")
    @Size(max = 500)
    private String shippingAddress;
}
