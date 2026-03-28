package com.ecommerce.dto;

import com.ecommerce.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Long id;
    private BigDecimal totalPrice;
    private OrderStatus status;
    private String shippingAddress;
    private Instant createdAt;
    private List<OrderItemDTO> items;
}
