package com.ecommerce.controller;

import com.ecommerce.dto.CartAddRequest;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartRemoveRequest;
import com.ecommerce.dto.CartUpdateRequest;
import com.ecommerce.security.SecurityUtils;
import com.ecommerce.service.CartService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartDTO getCart() {
        return cartService.getCart(SecurityUtils.currentUserId());
    }

    @PostMapping("/add")
    public CartDTO add(@Valid @RequestBody CartAddRequest request) {
        return cartService.addItem(SecurityUtils.currentUserId(), request);
    }

    @PutMapping("/update")
    public CartDTO update(@Valid @RequestBody CartUpdateRequest request) {
        return cartService.updateItem(SecurityUtils.currentUserId(), request);
    }

    @DeleteMapping("/remove")
    public CartDTO remove(@Valid @RequestBody CartRemoveRequest request) {
        return cartService.removeItem(SecurityUtils.currentUserId(), request);
    }
}
