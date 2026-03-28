package com.ecommerce.service;

import com.ecommerce.dto.OrderCreateRequest;
import com.ecommerce.dto.OrderDTO;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;
import com.ecommerce.exception.BusinessException;
import com.ecommerce.exception.ResourceNotFoundException;
import com.ecommerce.mapper.OrderMapper;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.repository.OrderRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createFromCart_throws_when_cart_empty() {
        Long userId = 1L;
        Cart cart = Cart.builder().id(1L).items(new ArrayList<>()).build();
        when(cartRepository.findByUser_Id(userId)).thenReturn(Optional.of(cart));

        OrderCreateRequest req = new OrderCreateRequest();
        req.setShippingAddress("123 St");

        assertThatThrownBy(() -> orderService.createFromCart(userId, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void createFromCart_creates_order_and_clears_cart() {
        Long userId = 1L;
        Product product = Product.builder()
                .id(10L)
                .name("P")
                .price(new BigDecimal("5.00"))
                .stockQuantity(100)
                .build();
        CartItem ci = CartItem.builder().product(product).quantity(2).build();
        List<CartItem> items = new ArrayList<>();
        items.add(ci);
        Cart cart = Cart.builder().id(1L).items(items).build();
        ci.setCart(cart);

        User user = User.builder().id(userId).email("u@u.com").build();
        when(cartRepository.findByUser_Id(userId)).thenReturn(Optional.of(cart));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(99L);
            return o;
        });
        when(orderRepository.findByIdWithDetails(99L)).thenAnswer(inv -> {
            Order o = Order.builder()
                    .id(99L)
                    .user(user)
                    .totalPrice(new BigDecimal("10.00"))
                    .status(OrderStatus.CREATED)
                    .shippingAddress("Addr")
                    .items(new ArrayList<>())
                    .build();
            return Optional.of(o);
        });
        when(orderMapper.toDto(any(Order.class))).thenReturn(
                OrderDTO.builder().id(99L).totalPrice(new BigDecimal("10.00")).status(OrderStatus.CREATED).build());

        OrderCreateRequest req = new OrderCreateRequest();
        req.setShippingAddress("Addr");

        OrderDTO dto = orderService.createFromCart(userId, req);

        assertThat(dto.getId()).isEqualTo(99L);
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void getByIdForUser_throws_when_not_found() {
        when(orderRepository.findByIdAndUser_Id(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getByIdForUser(2L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
