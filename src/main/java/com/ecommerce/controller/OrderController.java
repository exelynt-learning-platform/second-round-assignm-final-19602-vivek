package com.ecommerce.controller;

import com.ecommerce.dto.OrderCreateRequest;
import com.ecommerce.dto.OrderDTO;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO create(@Valid @RequestBody OrderCreateRequest request) {
        return orderService.createFromCart(SecurityUtils.currentUserId(), request);
    }

    @GetMapping
    public List<OrderDTO> list() {
        return orderService.listForUser(SecurityUtils.currentUserId());
    }

    @GetMapping("/{id}")
    public OrderDTO get(@PathVariable Long id) {
        return orderService.getByIdForUser(SecurityUtils.currentUserId(), id);
    }
}
