package com.ecommerce.service;

import com.ecommerce.dto.CartAddRequest;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartItemDTO;
import com.ecommerce.dto.ProductDTO;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.CartMapper;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.ProductRepository;
import com.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CartMapper cartMapper;

    @InjectMocks
    private CartService cartService;

    @Test
    void addItem_throws_when_quantity_exceeds_stock() {
        Long userId = 1L;
        Product product = Product.builder()
                .id(10L)
                .name("P1")
                .price(BigDecimal.TEN)
                .stockQuantity(2)
                .build();
        Cart cart = Cart.builder().id(5L).user(User.builder().id(userId).build()).items(new ArrayList<>()).build();
        when(cartRepository.findByUser_Id(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        CartAddRequest req = new CartAddRequest();
        req.setProductId(10L);
        req.setQuantity(5);

        assertThatThrownBy(() -> cartService.addItem(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void addItem_persists_new_line() {
        Long userId = 1L;
        Product product = Product.builder()
                .id(10L)
                .name("P1")
                .price(BigDecimal.TEN)
                .stockQuantity(10)
                .build();
        Cart cart = Cart.builder().id(5L).user(User.builder().id(userId).build()).items(new ArrayList<>()).build();
        when(cartRepository.findByUser_Id(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));
        CartDTO dto = CartDTO.builder()
                .id(5L)
                .items(List.of(CartItemDTO.builder()
                        .product(ProductDTO.builder().id(10L).price(BigDecimal.TEN).build())
                        .quantity(2)
                        .build()))
                .totalPrice(new BigDecimal("20"))
                .build();
        when(cartMapper.toDto(any(Cart.class))).thenReturn(dto);

        CartAddRequest req = new CartAddRequest();
        req.setProductId(10L);
        req.setQuantity(2);

        cartService.addItem(userId, req);

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void getOrCreateCartEntity_throws_when_user_missing() {
        when(cartRepository.findByUser_Id(99L)).thenReturn(Optional.empty());
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.getOrCreateCartEntity(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
