package com.ecommerce.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartRemoveRequest {

    @NotNull
    private Long productId;
}
