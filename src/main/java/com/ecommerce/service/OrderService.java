package com.ecommerce.service;

import com.ecommerce.dto.OrderCreateRequest;
import com.ecommerce.dto.OrderDTO;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    public OrderService(
            OrderRepository orderRepository,
            CartRepository cartRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderDTO createFromCart(Long userId, OrderCreateRequest request) {
        Cart cart = cartRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        if (cart.getItems().isEmpty()) {
            throw new BusinessException("Cart is empty");
        }
        for (CartItem ci : cart.getItems()) {
            Product p = productRepository.findById(ci.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            if (ci.getQuantity() > p.getStockQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + p.getName());
            }
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        Order order = Order.builder()
                .user(user)
                .status(OrderStatus.CREATED)
                .shippingAddress(request.getShippingAddress())
                .totalPrice(BigDecimal.ZERO)
                .build();
        for (CartItem ci : cart.getItems()) {
            Product p = productRepository.findById(ci.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            total = total.add(lineTotal);
            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .product(p)
                    .quantity(ci.getQuantity())
                    .price(p.getPrice())
                    .build();
            orderItems.add(oi);
        }
        order.setTotalPrice(total);
        order.setItems(orderItems);
        order = orderRepository.save(order);
        cart.getItems().clear();
        cartRepository.save(cart);
        log.info("Created order id={} userId={}", order.getId(), userId);
        Order withDetails = orderRepository.findByIdWithDetails(order.getId()).orElse(order);
        return orderMapper.toDto(withDetails);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> listForUser(Long userId) {
        return orderRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(orderMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderDTO getByIdForUser(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUser_Id(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return orderMapper.toDto(order);
    }
}
