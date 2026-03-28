package com.ecommerce.mapper;

import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CartMapper {

    private final ProductMapper productMapper;

    public CartMapper(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    public CartDTO toDto(Cart cart) {
        if (cart == null) {
            return null;
        }
        List<CartItemDTO> items = cart.getItems().stream()
                .map(this::itemToDto)
                .toList();
        BigDecimal total = items.stream()
                .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CartDTO.builder()
                .id(cart.getId())
                .items(items)
                .totalPrice(total)
                .build();
    }

    public CartItemDTO itemToDto(CartItem item) {
        return CartItemDTO.builder()
                .id(item.getId())
                .product(productMapper.toDto(item.getProduct()))
                .quantity(item.getQuantity())
                .build();
    }
}
