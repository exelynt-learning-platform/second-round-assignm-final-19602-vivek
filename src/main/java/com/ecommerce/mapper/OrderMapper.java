package com.ecommerce.mapper;

import com.ecommerce.dto.OrderDTO;
import com.ecommerce.dto.OrderItemDTO;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderMapper {

    private final ProductMapper productMapper;

    public OrderMapper(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public OrderDTO toDto(Order order) {
        if (order == null) {
            return null;
        }
        List<OrderItemDTO> items = order.getItems().stream()
                .map(this::itemToDto)
                .toList();
        return OrderDTO.builder()
                .id(order.getId())
                .totalPrice(order.getTotalPrice())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress())
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    public OrderItemDTO itemToDto(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .product(productMapper.toDto(item.getProduct()))
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .build();
    }
}
