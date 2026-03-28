package com.ecommerce.service;

import com.ecommerce.dto.CartAddRequest;
import com.ecommerce.dto.CartDTO;
import com.ecommerce.dto.CartRemoveRequest;
import com.ecommerce.dto.CartUpdateRequest;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CartMapper cartMapper;

    public CartService(
            CartRepository cartRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.cartMapper = cartMapper;
    }

    @Transactional(readOnly = true)
    public CartDTO getCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        return cartMapper.toDto(cart);
    }

    @Transactional
    public CartDTO addItem(Long userId, CartAddRequest request) {
        Cart cart = getOrCreateCartEntity(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        validateQuantityAgainstStock(product, request.getQuantity());
        CartItem existing = findItemByProduct(cart, product.getId());
        if (existing != null) {
            int newQty = existing.getQuantity() + request.getQuantity();
            validateQuantityAgainstStock(product, newQty);
            existing.setQuantity(newQty);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .build();
            cart.getItems().add(item);
        }
        cart = cartRepository.save(cart);
        log.debug("Cart add userId={} productId={}", userId, request.getProductId());
        return cartMapper.toDto(cart);
    }

    @Transactional
    public CartDTO updateItem(Long userId, CartUpdateRequest request) {
        Cart cart = cartRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        validateQuantityAgainstStock(product, request.getQuantity());
        CartItem existing = findItemByProduct(cart, product.getId());
        if (existing == null) {
            throw new BusinessException("Product not in cart");
        }
        existing.setQuantity(request.getQuantity());
        cart = cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }

    @Transactional
    public CartDTO removeItem(Long userId, CartRemoveRequest request) {
        Cart cart = cartRepository.findByUser_Id(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        boolean removed = cart.getItems().removeIf(
                ci -> ci.getProduct().getId().equals(request.getProductId()));
        if (!removed) {
            throw new BusinessException("Product not in cart");
        }
        cart = cartRepository.save(cart);
        return cartMapper.toDto(cart);
    }

    public Cart getOrCreateCartEntity(Long userId) {
        return cartRepository.findByUser_Id(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    private static CartItem findItemByProduct(Cart cart, Long productId) {
        return cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    private static void validateQuantityAgainstStock(Product product, int quantity) {
        if (quantity > product.getStockQuantity()) {
            throw new BusinessException("Insufficient stock for product: " + product.getName());
        }
        if (quantity < 1) {
            throw new BusinessException("Quantity must be at least 1");
        }
    }
}
